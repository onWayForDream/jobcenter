package cn.enn.portal.jobCenter.core.service;

import cn.enn.portal.jobCenter.PJobMetadata;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.core.*;
import cn.enn.portal.jobCenter.ScheduleType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.jobLauncher.JobLauncherProxy;
import cn.enn.portal.jobCenter.core.jobRunner.RepeatedPJobRunner;
import cn.enn.portal.jobCenter.core.entity.JobScheduleLogEntity;
import cn.enn.portal.jobCenter.core.jobRunner.RepeatedPJobResolverRunner;
import cn.enn.portal.jobCenter.core.repository.JobScheduleLogRepository;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import cn.enn.portal.jobCenter.core.zk.consumer.MaintenanceCommandExecutor;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStartProducer;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStopProducer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JobScheduleService {

    private final Logger logger = LoggerFactory.getLogger(JobScheduleService.class);

    private Scheduler quartzScheduler;

    public Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    private Map<String, RunningSchedule> runningSchedulers = new ConcurrentHashMap<>();

    @Autowired
    JobLauncherProxy jobLauncherProxy;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobScheduleLogRepository jobScheduleLogRepository;

    @Autowired
    JobRunService jobRunService;

    @Autowired
    FutureSchedulerManager futureSchedulerManager;

    @Autowired
    JobCenterCoreProperty jobCenterCoreProperty;

    @Autowired
    HostNameProvider hostNameProvider;

    @Autowired
    ZookeeperClient zookeeperClient;

    @Autowired
    ZkJobStopProducer zkJobStopProducer;

    @Autowired
    ZkJobStartProducer zkJobStartProducer;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    MaintenanceCommandExecutor shutdownCommandExecutor;


    public JobScheduleService() throws SchedulerException {
        quartzScheduler = new StdSchedulerFactory().getScheduler();

    }

    public JobScheduleLogEntity prepareToRunJob(JobEntity jobEntity,
                                                ScheduleActionType scheduleActionType,
                                                String operator,
                                                Optional<Date> overrideStartTime) throws Exception {
        Optional<ProjectEntity> project = projectRepository.findById(jobEntity.getProjectId());
        if (!project.isPresent()) {
            String msg = MessageFormat.format("project not found, project_id = {0}, job_id = {1}", jobEntity.getProjectId(), jobEntity.getId());
            throw new RuntimeException(msg);
        }
        return prepareToRunJob(project.get(), jobEntity, scheduleActionType, operator, overrideStartTime);
    }

    public JobScheduleLogEntity prepareToRunJob(ProjectEntity projectEntity,
                                                JobEntity jobEntity,
                                                ScheduleActionType scheduleActionType,
                                                String operator,
                                                Optional<Date> overrideStartTime) throws Exception {
        if (jobEntity.getDisabled() == 1) {
            return null;
        }
        Optional<Date> startTime = overrideStartTime;
        if (!startTime.isPresent() && jobEntity.getValidityFrom() != null) {
            startTime = Optional.of(jobEntity.getValidityFrom());
        }
        JobScheduleLogEntity resultEntity = null;
        if (startTime.isPresent() && startTime.get().compareTo(new Date()) > 0) {
            futureSchedulerManager.runInFuture(startTime.get(), this, projectEntity, jobEntity, scheduleActionType, operator);
            updateJobStatus(jobEntity.getId(), JobRunStatus.FUTURE_RUN.toString());
        } else {
            resultEntity = runJob(projectEntity, jobEntity, scheduleActionType, operator);
        }

        if (jobEntity.getValidityTo() != null) {
            futureSchedulerManager.shutdownInFuture(jobEntity.getValidityTo(), this, projectEntity, jobEntity, scheduleActionType, operator);
        }
        return resultEntity;
    }

    public JobScheduleLogEntity runJob(ProjectEntity projectEntity,
                                       JobEntity jobEntity,
                                       ScheduleActionType scheduleActionType,
                                       String operator) throws Exception {

        String scheduleKey = getScheduleKey(projectEntity, jobEntity);
        if (isJobRunningByKey(scheduleKey)) {
            String msg = MessageFormat.format("can not run job [{0}] because it is running! please stop it first",
                    jobEntity.getJobName());
            throw new UnsupportedOperationException(msg);
        }
        logger.trace("trace time after isJobRunningByKey");
        // load class from target job jars
        PJob pJob = null;
        PJobResolver pjobResolver = null;

        boolean isResolverTask = jobEntity.getIsResolverJob() == 1;
        if (!isResolverTask) {
            pJob = jobLauncherProxy.loadPJob(projectEntity, jobEntity);
        } else {
            pjobResolver = jobLauncherProxy.loadPJobResolver(projectEntity, jobEntity);
        }
        // write schedule info to database
        JobScheduleLogEntity jobScheduleLogEntity = new JobScheduleLogEntity();
        jobScheduleLogEntity.inflateBySchedulerEntity(jobEntity);
        jobScheduleLogEntity.setActionType(scheduleActionType == null ? "" : scheduleActionType.toString());
        jobScheduleLogEntity.setCreateTime(new Date());
        jobScheduleLogEntity.setRunningHost(hostNameProvider.getHostName());
        jobScheduleLogEntity.setOperator(operator);
        jobScheduleLogEntity.setUuid(UUID.randomUUID().toString());
//        jobScheduleLogRepository.save(jobScheduleLogEntity);
        AsyncRepositorySaver.<JobScheduleLogEntity>AsyncSave(jobScheduleLogRepository, jobScheduleLogEntity);

        logger.trace("trace time after jobScheduleLogRepository.save");

        ScheduleType jobScheduleType = ScheduleType.valueOf(jobScheduleLogEntity.getScheduleType());

        // schedule job with quartz
        JobDetail quartzJobDetail = null;
        if (!isResolverTask) {
            if (jobScheduleType.equals(ScheduleType.MANUAL)) {
                quartzJobDetail = JobBuilder.newJob(RepeatedPJobRunner.class).storeDurably().build();
            } else {
                quartzJobDetail = JobBuilder.newJob(RepeatedPJobRunner.class).build();
            }
            quartzJobDetail.getJobDataMap().put("pJob", pJob);
        } else {
            if (jobScheduleType.equals(ScheduleType.MANUAL)) {
                quartzJobDetail = JobBuilder.newJob(RepeatedPJobResolverRunner.class).storeDurably().build();
            } else {
                quartzJobDetail = JobBuilder.newJob(RepeatedPJobResolverRunner.class).build();
            }

            quartzJobDetail.getJobDataMap().put("pjobResolver", pjobResolver);
        }
        logger.trace("trace time after newJob");
        quartzJobDetail.getJobDataMap().put("projectEntity", projectEntity);
        quartzJobDetail.getJobDataMap().put("jobParam", jobEntity.getJobParamJson());
        quartzJobDetail.getJobDataMap().put("jobRunService", jobRunService);
        quartzJobDetail.getJobDataMap().put("jobEntity", jobEntity);
        quartzJobDetail.getJobDataMap().put("jobScheduleLogEntity", jobScheduleLogEntity);
        quartzJobDetail.getJobDataMap().put("jobRepository", jobRepository);
        quartzJobDetail.getJobDataMap().put("jobScheduleService", this);
        quartzJobDetail.getJobDataMap().put("isRunOnce", jobScheduleType.equals(ScheduleType.MANUAL));
        quartzJobDetail.getJobDataMap().put("jobCenterCoreProperty", jobCenterCoreProperty);
        quartzJobDetail.getJobDataMap().put("hostNameProvider", hostNameProvider);
        quartzJobDetail.getJobDataMap().put("zookeeperClient", zookeeperClient);
        quartzJobDetail.getJobDataMap().put("zkJobStartProducer", zkJobStartProducer);
        quartzJobDetail.getJobDataMap().put("zkJobStopProducer", zkJobStopProducer);
        quartzJobDetail.getJobDataMap().put("shutdownCommandExecutor", shutdownCommandExecutor);


        // load properties file and profile-based properties file
        JobEntity propertiesInfoJob = jobEntity;
        if (jobEntity.getResolverId() > 0) {
            propertiesInfoJob = jobRepository.findById(jobEntity.getResolverId()).get();
        }
        try {
            Properties configProperties = jobLauncherProxy.loadConfigProperties(projectEntity, propertiesInfoJob);
            quartzJobDetail.getJobDataMap().put("configProperties", configProperties);
        } catch (IOException ex) {
            logger.error("load config properties failed!", ex);
        }

        Trigger quartzTrigger = null;

        switch (jobScheduleType) {
            case INTERVAL:
                quartzTrigger = getIntervalTrigger(jobScheduleLogEntity.getScheduleValue());
                break;
            case CRON_EXPRESSION:
                quartzTrigger = getCronExprTrigger(jobScheduleLogEntity.getScheduleValue());
                break;
            case MANUAL:
                break;
            default:
                throw new UnsupportedOperationException(
                        "unsupported schedule type :" + jobScheduleLogEntity.getScheduleType());
        }
        logger.trace("trace time after create trigger");
//            quartzScheduler.start();
        if (jobScheduleType.equals(ScheduleType.MANUAL)) {
            quartzScheduler.addJob(quartzJobDetail, true);
            quartzScheduler.triggerJob(quartzJobDetail.getKey());
        } else {
            quartzScheduler.scheduleJob(quartzJobDetail, quartzTrigger);
        }
        logger.info(MessageFormat.format("schedule job [{0}] with scheduler [{1}] success", projectEntity.getName(), jobEntity.getJobName()));
        logger.trace("trace time after schedule job");
        // save quartz jobs into map for delete job
        runningSchedulers.put(scheduleKey, new RunningSchedule(scheduleKey, jobEntity, jobScheduleLogEntity, quartzJobDetail.getKey()));
        logger.trace("trace time after runningSchedulers.put");

        // update job status
        updateJobStatus(jobEntity.getId(), JobRunStatus.RUNNING.toString());
        return jobScheduleLogEntity;
    }

    private Trigger getIntervalTrigger(String intervalString) {
        long milliSeconds = Long.parseLong(intervalString);
        Trigger trigger = TriggerBuilder
                .newTrigger().withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withIntervalInMilliseconds(milliSeconds).repeatForever())
                .build();
        return trigger;
    }

    private Trigger getCronExprTrigger(String cronExpr) {
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr))
                .build();
        return trigger;
    }

    public void shutdownJob(JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator, boolean recursive) {
        ProjectEntity project = projectRepository.findById(jobEntity.getProjectId()).get();
        shutdownJob(project, jobEntity, scheduleActionType, operator, recursive);
    }

    public void shutdownJob(ProjectEntity projectEntity, JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator, boolean recursive) {

        // check is there any future task with this scheduler
        if (futureSchedulerManager.isRunInfuture(projectEntity.getId(), jobEntity.getId())) {
            futureSchedulerManager.cancelRun(projectEntity.getId(), jobEntity.getId());
        }
        if (futureSchedulerManager.isShutdownInFuture(projectEntity.getId(), jobEntity.getId())) {
            futureSchedulerManager.cancelShutdown(projectEntity.getId(), jobEntity.getId());
        }

        String scheduleKey = getScheduleKey(projectEntity, jobEntity);
        if (!isJobRunningByKey(scheduleKey)) {
//            String msg = MessageFormat.format("job [{0}] with scheduler [{1}] is not be scheduled at this moment",
//                    projectEntity.getName(), jobEntity.getJobName());
//            throw new UnsupportedOperationException(msg);
//            return null;
        } else {
            RunningSchedule runningSchedule = runningSchedulers.get(scheduleKey);
            JobKey jobKey = runningSchedule.getQuartzJobKey();
            try {
                quartzScheduler.interrupt(jobKey);
                quartzScheduler.deleteJob(jobKey);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            runningSchedulers.remove(scheduleKey);

            JobScheduleLogEntity jobScheduleLogEntity = new JobScheduleLogEntity();
            jobScheduleLogEntity.inflateBySchedulerEntity(jobEntity);
            jobScheduleLogEntity.setActionType(scheduleActionType == null ? "" : scheduleActionType.toString());
            jobScheduleLogEntity.setCreateTime(new Date());
            jobScheduleLogEntity.setRunningHost(hostNameProvider.getHostName());
            jobScheduleLogEntity.setOperator(operator);
//        jobScheduleLogRepository.save(jobScheduleLogEntity);
            AsyncRepositorySaver.<JobScheduleLogEntity>AsyncSave(jobScheduleLogRepository, jobScheduleLogEntity);

            // 如果是resolver的话，递归shutdown其创建的所有正在运行的任务
            if (recursive && jobEntity.getIsResolverJob() == 1) {
                List<JobEntity> subTasks = jobRepository.findByResolverId(jobEntity.getId());
                for (JobEntity subTask : subTasks) {
                    if (isJobRunning(subTask)) {
                        try {
//                          shutdownJob(projectEntity, subTask, ScheduleActionType.RESOLVER_SHUTDOWN, operator);
                            zkJobStopProducer.stopJob(subTask, ScheduleActionType.RESOLVER_SHUTDOWN, operator, false);
                            logger.info("scheduler is shutdown:{} ", subTask.getJobName());
                        } catch (Exception e) {
                            logger.error("stop job by zookeeper failed", e);
                        }
                    } else {
                        logger.info("scheduler don't need to shutdown because it is not running:{},status:{}",
                                subTask.getJobName(), subTask.getStatus());
                    }
                }
            }
        }
        updateJobStatus(jobEntity.getId(), JobRunStatus.DONE.toString());

//        return jobScheduleLogEntity;
    }

    private String getScheduleKey(ProjectEntity projectEntity, JobEntity jobEntity) {
        return projectEntity.getId() + "$$$$" + jobEntity.getId();
    }

    private boolean isJobRunningByKey(String scheduleKey) {
        return runningSchedulers.containsKey(scheduleKey);
    }

    public boolean isJobRunning(JobEntity jobEntity) {
        if (jobEntity.getStatus() == null) {
            return false;
        }
        if (futureSchedulerManager.isRunInfuture(jobEntity.getProjectId(), jobEntity.getId())) {
            return true;
        }
        return !jobEntity.getStatus().equals(JobRunStatus.DONE.toString());
    }

    public List<JobEntity> getRunningJobs(int projectId) {
        List<JobEntity> scheduleList = new ArrayList<>();
        for (String key : runningSchedulers.keySet()) {
            RunningSchedule _item = runningSchedulers.get(key);
            if (_item.getJobEntity().getProjectId() == projectId) {
                scheduleList.add(_item.getJobEntity());
            }
        }
        return scheduleList;
    }

    public List<JobEntity> getAllRunningJobs() {
        return this.runningSchedulers.values().stream().map(s -> s.getJobEntity()).collect(Collectors.toList());
    }


    @Transactional
    public void disableJobById(int jobId) {
        JobEntity jobEntity = jobRepository.findById(jobId).get();
        jobEntity.setDisabled(1);
        jobRepository.save(jobEntity);
    }

    @Transactional
    public JobEntity updateJobMetadata(PJobMetadata jobMetadata, int jobId) {
        JobEntity jobEntity = jobRepository.findById(jobId).get();
        jobEntity.setExecuteClass(jobMetadata.getExecuteClass());
        jobEntity.setJobParamJson(jobMetadata.getJobParamJson());
        jobEntity.setJobName(jobMetadata.getJobName());
        jobEntity.setScheduleValue(jobMetadata.getScheduleValue());
        jobEntity.setScheduleType(jobMetadata.getScheduleType().toString());
        jobEntity.setValidityFrom(jobMetadata.getValidityFrom());
        jobEntity.setValidityTo(jobMetadata.getValidityTo());
        jobEntity.setUpdateTime(new Date());
        jobEntity.setConcurrentOpt(jobMetadata.getConcurrentOpt().toString());
        jobEntity.setRuntimeType(jobMetadata.getRuntimeTime());
        jobEntity.setIdentifierCode(jobMetadata.getIdentifierCode());
        jobEntity.setDisabled(0);
        return jobRepository.save(jobEntity);
    }

    @Transactional
    public void updateJobStatus(int id, String jobStatus) {
        JobEntity jobEntity = jobRepository.findById(id).get();
        jobEntity.setStatus(jobStatus);
        jobEntity.setRunningHost(hostNameProvider.getHostName());
        jobRepository.save(jobEntity);
    }

    @Transactional
    public void updateJobStatusWhen(int id, String jobStatus, Collection<String> compareStatus) {
        JobEntity jobEntity = jobRepository.findById(id).get();
        if (compareStatus.contains(jobEntity.getStatus())) {
            jobEntity.setStatus(jobStatus);
            jobRepository.save(jobEntity);
        }

    }

    /**
     * 计算一下重新启动job时，是否需要修改启动时间
     *
     * @param job
     * @return 如果需要修改启动时间，则返回这个时间，否则，返回null
     */
    public Date getOverrideRestartTime(JobEntity job) {
        //如果job没有调度起来，则不计算的重新启动的时间
        List<JobEntity> runningJobs = getRunningJobs(job.getProjectId());
        if (runningJobs == null || !runningJobs.contains(job)) {
            return null;
        }

        // 目前来看，只有设置了明确的开始时间，且已经过了这个开始时间，并且以INTERVAL形式来调度的任务，才需要修改重新启动的时间。
        // 例如：我希望一个job在某天下午3:00开始运行，然后每半小时运行一次，这种情况下，用户是希望整点或半点启动的，这就需要将开始时间明确地设置为下一个整点或半点
        // 如果用户希望在某天下午3:00开始运行，但那天还没到，则无需修改启动时间
        // 如果job是以cron表达式的形式来调度的，也无需修改开始时间
        if (job.getValidityFrom() != null
                && job.getValidityFrom().compareTo(new Date()) <= 0
                && job.getScheduleType().equals(ScheduleType.INTERVAL.toString())) {
            // 计算下一次触发的时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(job.getValidityFrom());
            Date date = calendar.getTime();
            while (date.compareTo(new Date()) < 0) {
                calendar.add(Calendar.MILLISECOND, Integer.parseInt(job.getScheduleValue()));
                date = calendar.getTime();
            }
            return date;
        }
        return null;
    }

    @Transactional
    public JobEntity enableJob(int projectId, String jobName) {
        JobEntity job = jobRepository.findDisabledJob(projectId, jobName);
        if (job != null) {
            job.setDisabled(0);
            return jobRepository.save(job);
        }
        return null;
    }

    class RunningSchedule {

        public RunningSchedule(String runningKey, JobEntity jobEntity, JobScheduleLogEntity jobScheduleLogEntity, JobKey quartzJobKey) {
            this.runningKey = runningKey;
            this.jobEntity = jobEntity;
            this.jobScheduleLogEntity = jobScheduleLogEntity;
            this.quartzJobKey = quartzJobKey;
        }

        private String runningKey;
        private JobEntity jobEntity;
        private JobScheduleLogEntity jobScheduleLogEntity;
        private JobKey quartzJobKey;

        public String getRunningKey() {
            return runningKey;
        }

        public void setRunningKey(String runningKey) {
            this.runningKey = runningKey;
        }

        public JobEntity getJobEntity() {
            return jobEntity;
        }

        public void setJobEntity(JobEntity jobEntity) {
            this.jobEntity = jobEntity;
        }

        public JobKey getQuartzJobKey() {
            return quartzJobKey;
        }

        public void setQuartzJobKey(JobKey quartzJobKey) {
            this.quartzJobKey = quartzJobKey;
        }

        public JobScheduleLogEntity getJobScheduleLogEntity() {
            return jobScheduleLogEntity;
        }

        public void setJobScheduleLogEntity(JobScheduleLogEntity jobScheduleLogEntity) {
            this.jobScheduleLogEntity = jobScheduleLogEntity;
        }
    }

}
