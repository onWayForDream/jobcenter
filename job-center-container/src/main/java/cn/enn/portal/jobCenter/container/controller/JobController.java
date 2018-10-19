package cn.enn.portal.jobCenter.container.controller;


import cn.enn.portal.jobCenter.ScheduleType;
import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.exception.FieldRequiredException;
import cn.enn.portal.jobCenter.container.exception.UnauthorizedException;
import cn.enn.portal.jobCenter.container.viewmodel.JobDetailModel;
import cn.enn.portal.jobCenter.container.viewmodel.JobListItemViewModel;
import cn.enn.portal.jobCenter.core.FutureSchedulerManager;
import cn.enn.portal.jobCenter.core.JobRunStatus;
import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.SupportedRuntime;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.container.viewmodel.JobPostModel;
import cn.enn.portal.jobCenter.core.exception.IncompatibleClassException;
import cn.enn.portal.jobCenter.core.jobLauncher.JobLauncherProxy;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.service.ProjectService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStartProducer;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStopProducer;
import org.apache.zookeeper.KeeperException;
import org.json.JSONObject;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{project_id}/jobs")
public class JobController {

    @Autowired
    JobScheduleService jobScheduleService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobLauncherProxy jobLauncherProxy;

    @Autowired
    FutureSchedulerManager futureSchedulerManager;

    @Autowired
    JobRunLogRepository jobRunLogRepository;

    @Autowired
    ZkJobStartProducer zkJobStartProducer;

    @Autowired
    ZkJobStopProducer zkJobStopProducer;

    @Autowired
    HostNameProvider hostNameProvider;

    @Autowired
    ProjectService projectService;

    @PostMapping("")
    public JobEntity newJob(@RequestBody JobPostModel postModel,
                            @PathVariable(name = "project_id") int projectId,
                            HttpSession session)
            throws Exception {

        if (!projectService.checkAuthorization(projectId, session)) {
            throw new UnauthorizedException();
        }

        boolean isResolver = checkJobAndReturnsIsResolver(projectId, postModel);

        if (jobRepository.existsByProjectIdAndJobName(projectId, postModel.getJobName())) {
            throw new ContainerException(MessageFormat.format("job name [{0}] already exist in current project", postModel.getJobName()), HttpStatus.BAD_REQUEST);
        }

        JobEntity jobEntity = new JobEntity();
        jobEntity.setCreateTime(new Date());
        jobEntity.setUpdateTime(new Date());
        jobEntity.setExecuteClass(postModel.getExecuteClass());
        jobEntity.setIsResolverJob(isResolver ? 1 : 0);
        jobEntity.setProjectId(projectId);
        jobEntity.setJobParamJson(postModel.getJobParamJson());
        jobEntity.setOwner("");
        jobEntity.setJobName(postModel.getJobName());
        jobEntity.setScheduleValue(postModel.getScheduleValue());
        jobEntity.setScheduleType(postModel.getScheduleType());
        jobEntity.setValidityFrom(postModel.getValidityFrom());
        jobEntity.setValidityTo(postModel.getValidityTo());
        jobEntity.setConcurrentOpt(postModel.getConcurrentOpt().toString());
        jobEntity.setPropertiesFile(postModel.getPropertiesFile());
        jobEntity.setProfileName(postModel.getProfileName());
        jobEntity.setRuntimeType(postModel.getRuntimeType());
        jobEntity.setRunningHost(hostNameProvider.getHostName());
        jobEntity.setStatus(JobRunStatus.DONE.toString());
        jobRepository.save(jobEntity);
//        startJob(projectId, jobEntity.getId());
        return jobEntity;
    }

    @Transactional
    @PutMapping("")
    public void updateJob(@RequestBody JobPostModel postModel,
                          @PathVariable(name = "project_id") int projectId,
                          HttpSession session) throws Exception {
        if (postModel.getId() <= 0) {
            throw new ContainerException("id must greater than 0", HttpStatus.BAD_REQUEST);
        }

        if (!projectService.checkAuthorization(projectId, session)) {
            throw new UnauthorizedException();
        }

        boolean isResolver = checkJobAndReturnsIsResolver(projectId, postModel);

        if (jobRepository.countByExistJob(projectId, postModel.getJobName(), postModel.getId()) > 0) {
            throw new ContainerException(MessageFormat.format("job name [{0}] already exist in current project", postModel.getJobName()), HttpStatus.BAD_REQUEST);
        }
        JobEntity jobEntity = jobRepository.getOne(postModel.getId());

        if (jobScheduleService.isJobRunning(jobEntity)) {
            throw new ContainerException(MessageFormat.format("job [{0}] is running, please shutdown it before update", postModel.getJobName()),
                    HttpStatus.BAD_REQUEST);
        }

        jobEntity.setExecuteClass(postModel.getExecuteClass());
        jobEntity.setIsResolverJob(isResolver ? 1 : 0);
        jobEntity.setProjectId(projectId);
        jobEntity.setJobParamJson(postModel.getJobParamJson());
        jobEntity.setOwner("");
        jobEntity.setJobName(postModel.getJobName());
        jobEntity.setScheduleValue(postModel.getScheduleValue());
        jobEntity.setScheduleType(postModel.getScheduleType());
        jobEntity.setValidityFrom(postModel.getValidityFrom());
        jobEntity.setValidityTo(postModel.getValidityTo());
        jobEntity.setUpdateTime(new Date());
        jobEntity.setConcurrentOpt(postModel.getConcurrentOpt().toString());
        jobEntity.setPropertiesFile(postModel.getPropertiesFile());
        jobEntity.setProfileName(postModel.getProfileName());
        jobEntity.setRuntimeType(postModel.getRuntimeType());
        jobRepository.save(jobEntity);
//        startJob(projectId, jobEntity.getId());
    }

    @PostMapping("/{job_id}/start")
    public void startJob(
            @PathVariable(name = "project_id") int projectId,
            @PathVariable(name = "job_id") int jobId,
            HttpSession session
    ) throws ContainerException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        if (futureSchedulerManager.isRunInfuture(projectId, jobId)) {
            throw new ContainerException("job is already in cached, do not run again:" + jobEntity.getJobName(), HttpStatus.BAD_REQUEST);
        }
        if (jobScheduleService.isJobRunning(jobEntity)) {
            throw new ContainerException("job is already running, do not run again:" + jobEntity.getJobName(), HttpStatus.BAD_REQUEST);
        }
        try {
//            jobScheduleLogEntity = jobScheduleService.prepareToRunJob(projectEntity, jobEntity, ScheduleActionType.USER_SCHEDULE, "");
            zkJobStartProducer.startJob(jobEntity, ScheduleActionType.USER_SCHEDULE, "");
        } catch (Exception e) {
            throw new ContainerException("fail to start job", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @GetMapping("/running-jobs")
    public List<JobEntity> listAllRunningJobsOfProject(@PathVariable("project_id") int projectId, HttpSession session) throws UnauthorizedException {
        if (!projectService.checkAuthorization(projectId, session)) {
            throw new UnauthorizedException();
        }
        return jobScheduleService.getRunningJobs(projectId);
    }

    @PostMapping("/{job_id}/shutdown")
    public void shutdownJob(
            @PathVariable(name = "project_id") int projectId,
            @PathVariable(name = "job_id") int jobId,
            HttpSession session) throws SchedulerException, ContainerException, IOException, InterruptedException, KeeperException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
//        jobScheduleService.shutdownJob(projectEntity, jobEntity, ScheduleActionType.USER_SHUTDOWN, "");
        zkJobStopProducer.stopJob(jobEntity, ScheduleActionType.USER_SHUTDOWN, "", true);
    }

    @GetMapping("")
    public Page<JobListItemViewModel> listAllJobsOfProject(
            @PathVariable(name = "project_id") int projectId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "") String kw,
            @RequestParam(required = false, defaultValue = "") String status,
            HttpSession session
    ) throws UnauthorizedException {
        if (!projectService.checkAuthorization(projectId, session)) {
            throw new UnauthorizedException();
        }
        int pageSize = 20;
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        Page<JobEntity> jobs = null;
        if (kw.isEmpty() && status.isEmpty()) {
            jobs = jobRepository.findByProjectId(projectId, pageable);
        } else if (!kw.isEmpty() && status.isEmpty()) {
            jobs = jobRepository.findByProjectIdAndJobName(projectId, kw, pageable);
        } else if (kw.isEmpty() && !status.isEmpty()) {
            jobs = jobRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else {
            jobs = jobRepository.findByProjectIdAndStatusAndJobName(projectId, status, kw, pageable);
        }

        return jobs.map(jobEntity -> {
            int subJobCount = 0;
            if (jobEntity.getIsResolverJob() == 1) {
                subJobCount = jobRepository.countByResolverId(jobEntity.getId());
            }
            return new JobListItemViewModel(jobEntity, subJobCount);
        });
    }

    @GetMapping("/subjobs/{resolver_id}")
    public List<JobListItemViewModel> listSubJobs(
            @PathVariable(name = "project_id") int projectId,
            @PathVariable("resolver_id") int resolverId,
            HttpSession session) throws ContainerException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        List<JobEntity> jobs = jobRepository.findByResolverId(resolverId);
        return jobs.stream().map(job -> new JobListItemViewModel(job)).collect(Collectors.toList());
    }

    @GetMapping("/{job_id}")
    public JobDetailModel getJobDetail(@PathVariable(name = "project_id") int projectId,
                                       @PathVariable(name = "job_id") int jobId,
                                       HttpSession session) throws ContainerException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        return new JobDetailModel(jobEntity, jobRunLogRepository);
    }


    private boolean checkJobAndReturnsIsResolver(int projectId, JobPostModel postModel) throws Exception {
        if (postModel.getJobName() == null || postModel.getJobName().isEmpty()) {
            throw new FieldRequiredException("jobName");
        }
        if (postModel.getExecuteClass() == null || postModel.getExecuteClass().isEmpty()) {
            throw new FieldRequiredException("executeClass");
        }
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);

//        if (postModel.getJobParamJson() != null && !postModel.getJobParamJson().isEmpty()) {
//            try {
//                jsonParam = new JSONObject(postModel.getJobParamJson());
//            } catch (Exception ex) {
//                throw new ContainerException("job_param_json is not in json format", HttpStatus.BAD_REQUEST);
//            }
//        }

        if (!Arrays.stream(ScheduleType.values()).anyMatch(scheduleType -> scheduleType.toString().equals(postModel.getScheduleType()))) {
            throw new ContainerException("invalid schedule_type",
                    HttpStatus.BAD_REQUEST);
        }


        if (postModel.getScheduleType().equals(ScheduleType.INTERVAL.toString())) {
            int milliSeconds = Integer.parseInt(postModel.getScheduleValue());
            if (milliSeconds < 5000) {
                throw new ContainerException("interval must greater than 5000", HttpStatus.BAD_REQUEST);
            }
        }

        boolean isPJob = false;
        boolean isResolver = false;
        JobEntity testJobEntity = new JobEntity();
        testJobEntity.setJobName(postModel.getJobName());
        testJobEntity.setRuntimeType(postModel.getRuntimeType());
        testJobEntity.setExecuteClass(postModel.getExecuteClass());
        try {
            jobLauncherProxy.loadPJob(projectEntity, testJobEntity);
            isPJob = true;
        } catch (IncompatibleClassException | ClassCastException _e) {
            // do nothing
        }
        try {
            jobLauncherProxy.loadPJobResolver(projectEntity, testJobEntity);
            isResolver = true;
        } catch (IncompatibleClassException | ClassCastException _e) {
            // do nothing
        }
        if (!isPJob && !isResolver) {
            throw new ContainerException("class " + postModel.getExecuteClass() + " must implement cn.enn.portal.jobCenter.PJob or cn.enn.portal.jobCenter.PJobResolver interface",
                    HttpStatus.BAD_REQUEST);
        }
        if (isPJob && isResolver) {
            throw new ContainerException("class " + postModel.getExecuteClass() + " must implement only one interface from cn.enn.portal.jobCenter.PJob or cn.enn.portal.jobCenter.PJobResolver",
                    HttpStatus.BAD_REQUEST);
        }
        return isResolver;

    }

    @DeleteMapping("/{job_id}")
    public void deleteJob(@PathVariable(name = "project_id") int projectId,
                          @PathVariable(name = "job_id") int jobId,
                          HttpSession session) throws ContainerException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        if (jobScheduleService.isJobRunning(jobEntity)) {
            throw new ContainerException("can not delete running job, please stop it first", HttpStatus.BAD_REQUEST);
        }
        if (jobEntity.getResolverId() > 0) {
            throw new ContainerException("can not delete job created by job resolver", HttpStatus.BAD_REQUEST);
        }
        List<Integer> idList = new ArrayList<>();
        idList.add(jobId);
        if (jobEntity.getIsResolverJob() == 1) {
            List<JobEntity> subJobs = jobRepository.findByResolverId(jobId);
            idList.addAll(subJobs.stream().map(job -> job.getId()).collect(Collectors.toList()));
        }
        jobRepository.deleteByIdIn(idList);
    }


}
