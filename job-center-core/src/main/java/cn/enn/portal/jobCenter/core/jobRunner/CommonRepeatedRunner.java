package cn.enn.portal.jobCenter.core.jobRunner;

import cn.enn.portal.jobCenter.ConcurrentOpt;
import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.ScheduleType;
import cn.enn.portal.jobCenter.core.JobCenterCoreProperty;
import cn.enn.portal.jobCenter.core.JobRunStatus;
import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.entity.JobScheduleLogEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.service.JobRunService;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.consumer.MaintenanceCommandExecutor;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStartProducer;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStopProducer;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

public abstract class CommonRepeatedRunner implements Job, InterruptableJob {
    private static ExecutorService jobThreadPool = Executors.newCachedThreadPool();
    protected final Logger logger = LoggerFactory.getLogger(CommonRepeatedRunner.class);
    protected PJob pJob;
    protected PJobResolver resolverInstance;
    protected ProjectEntity projectEntity;
    protected JobRunService jobRunService;
    protected JobEntity jobEntity;
    protected JobScheduleLogEntity jobScheduleLogEntity;
    protected JobScheduleService jobScheduleService;
    protected JobCenterCoreProperty jobCenterCoreProperty;
    protected String jobParam;
    protected JobRepository jobRepository;
    protected UUID runId;
    protected String runIdStr;
    protected Properties configProperties;
    protected HostNameProvider hostNameProvider;
    protected ZkJobStartProducer zkJobStartProducer;
    protected ZkJobStopProducer zkJobStopProducer;
    private MaintenanceCommandExecutor shutdownCommandExecutor;

    private volatile boolean isInterrupted = false;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobRunLogEntity jobRunEntity = null;
        boolean success = false;
        runId = UUID.randomUUID();
        Lock jobLock = null;
        runIdStr = "[" + runId + "]";
        try {

            jobEntity = (JobEntity) context.getJobDetail().getJobDataMap().get("jobEntity");
            projectEntity = (ProjectEntity) context.getJobDetail().getJobDataMap().get("projectEntity");
            // 通知PJobRunLock开始调度运行
            PJobRunLock.notifyActive(jobEntity);

            pJob = (PJob) context.getJobDetail().getJobDataMap().get("pJob");
            resolverInstance = (PJobResolver) context.getJobDetail().getJobDataMap().get("pjobResolver");
            jobRunService = (JobRunService) context.getJobDetail().getJobDataMap().get("jobRunService");
            jobParam = (String) context.getJobDetail().getJobDataMap().get("jobParam");
            if (jobParam == null) {
                jobParam = "";
            }
            jobScheduleLogEntity = (JobScheduleLogEntity) context.getJobDetail().getJobDataMap().get("jobScheduleLogEntity");
            jobScheduleService = (JobScheduleService) context.getJobDetail().getJobDataMap().get("jobScheduleService");
            jobCenterCoreProperty = (JobCenterCoreProperty) context.getJobDetail().getJobDataMap().get("jobCenterCoreProperty");
            jobRepository = (JobRepository) context.getJobDetail().getJobDataMap().get("jobRepository");
            configProperties = (Properties) context.getJobDetail().getJobDataMap().get("configProperties");
            hostNameProvider = (HostNameProvider) context.getJobDetail().getJobDataMap().get("hostNameProvider");
            zkJobStartProducer = (ZkJobStartProducer) context.getJobDetail().getJobDataMap().get("zkJobStartProducer");
            zkJobStopProducer = (ZkJobStopProducer) context.getJobDetail().getJobDataMap().get("zkJobStopProducer");


            shutdownCommandExecutor = (MaintenanceCommandExecutor) context.getJobDetail().getJobDataMap().get("shutdownCommandExecutor");
//            if (shutdownCommandExecutor.GLOBAL_SHUTDOWN_FLAG) {
//                throw new RuntimeException("当前主机停机维护，不再运行新任务");
//            }

            // for concurrent options:
            ConcurrentOpt concurrentOpt = ConcurrentOpt.valueOf(jobEntity.getConcurrentOpt());
            if (!concurrentOpt.equals(ConcurrentOpt.JustRunIt)) {
                // get lock for this job
                jobLock = PJobRunLock.getLockForJob(jobEntity.getId());
                if (concurrentOpt.equals(ConcurrentOpt.SuspendAndWait)) {
                    jobLock.lock();
                } else if (concurrentOpt.equals(ConcurrentOpt.SkipThisTime)) {
                    if (!jobLock.tryLock()) {
                        throw new RuntimeException("another job instance is running");
                    }
                }
            }

            jobRunEntity = new JobRunLogEntity();
            jobRunEntity.setProjectId(projectEntity.getId());
            jobRunEntity.setJobStartTime(new Date());
            jobRunEntity.setJobId(jobEntity.getId());
            jobRunEntity.setScheduleLogId(jobScheduleLogEntity.getUuid());
            jobRunEntity.setJobStatusCode(JobRunStatus.RUNNING.toString());
            jobRunEntity.setRunId(runId.toString());
            jobRunEntity.setRunningHost(hostNameProvider.getHostName());

            // write to database in status running
            jobRunService.save(jobRunEntity);


            logger.info(runIdStr + "begin to run job:" + jobEntity.getJobName());
//            String runResult = this.runUserJob();  // abstract method
            // 将实际执行用户job的代码放到一个线程中去执行，并不断响应用户的interrupt请求，
            Future<String> futureResult = jobThreadPool.submit(() -> this.runUserJob());
            String runResult = "";
            boolean runComplete = false;
            while (true) {
                try {
                    runResult = futureResult.get(1, TimeUnit.SECONDS);
                    // 运行到此处，说明userJob已经正常执行成功了
                    runComplete = true;
                    break;
                } catch (TimeoutException timeoutException) {
                    // timeout ，说明任务正在运行，还没有结束
                    ;
                }
                if (!runComplete && this.isInterrupted) {
                    // userJob尚未结束，但用户停止了job，就结束正在运行的userJob线程，并抛出异常，以此来跳出while循环并记录任务失败
                    futureResult.cancel(true);
                    throw new InterruptedException(runIdStr + "job is interrupted by user");
                }
            }
            logger.info(runIdStr + "job run success :" + jobEntity.getJobName());

            // update job_run item ,set status to done
            jobRunService.updateJobRunStatus(JobRunStatus.DONE, runResult, jobRunEntity.getId());
            success = true;

        } catch (Exception ex) {
            try {
                logger.error(runIdStr + "job failed :" + projectEntity.getName(), ex);

                // update job_run item ,set status to failed
                if (jobRunEntity != null) {
                    jobRunService.updateJobRunStatus(JobRunStatus.FAILED, ex.getMessage(), jobRunEntity.getId());
                }

                jobScheduleService.updateJobStatusWhen(jobEntity.getId(), JobRunStatus.FAILED.toString(),
                        Arrays.asList(JobRunStatus.RUNNING.toString()));
            } catch (Exception _e) {
                logger.error(runIdStr + "error in catch block", _e);
            }
        } finally {
            try {
                if (context.getJobDetail().getJobDataMap().getBoolean("isRunOnce")) {
                    zkJobStopProducer.stopJob(jobEntity, ScheduleActionType.MANUAL_STOP, jobScheduleLogEntity.getOperator(), true);
                }
                if (success) {
                    jobScheduleService.updateJobStatusWhen(jobEntity.getId(), JobRunStatus.RUNNING.toString(),
                            Arrays.asList(JobRunStatus.FAILED.toString()));
                }
            } catch (Exception ex) {
                logger.error(runIdStr + "error in finally block", ex);
            }

            if (jobLock != null) {
                jobLock.unlock();
            }

            // 通知PJobRunLock，本次运行结束
            PJobRunLock.notifySuspend(jobEntity);
        }

        // 检查是否需要将任务转移到其它节点
        if (!jobEntity.getScheduleType().equals(ScheduleType.MANUAL.toString())
                && shutdownCommandExecutor.GLOBAL_SHUTDOWN_FLAG) {
            if (!PJobRunLock.isJobActive(jobEntity.getId())) {
                try {
                    shutdownCommandExecutor.rerunJob(jobEntity);
                    logger.info("rerun job success in another host:{}", jobEntity.getJobName());
                } catch (Exception ex) {
                    logger.error("rerun job failed" + jobEntity.getJobName(), ex);
                }
            }
        }
    }

    public abstract String runUserJob() throws Exception;

//    private String


    @Override
    public void interrupt() throws UnableToInterruptJobException {
        this.isInterrupted = true;
        logger.warn("{}job {} is interrupted", runIdStr, jobEntity.getJobName());
    }
}
