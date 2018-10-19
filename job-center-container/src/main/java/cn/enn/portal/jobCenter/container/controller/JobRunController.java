package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.exception.UnauthorizedException;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.service.JobRunService;
import cn.enn.portal.jobCenter.core.service.ProjectService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobLogProducer;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{project_id}/jobs/{job_id}")
public class JobRunController {

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobRunLogRepository jobRunLogRepository;

    @Autowired
    ZookeeperClient zookeeperClient;

    @Autowired
    HostNameProvider hostNameProvider;

    @Autowired
    JobRunService jobRunService;

    @Autowired
    ZkJobLogProducer zkJobLogProducer;

    @Autowired
    ProjectService projectService;

    private Logger logger = LoggerFactory.getLogger(JobRunController.class);

    @GetMapping("/run_history")
    public Page<JobRunLogEntity> getRunLogList(
            @PathVariable(name = "project_id") int projectId,
            @PathVariable(name = "job_id") int jobId,
            @RequestParam(required = false, defaultValue = "1") int page,
            HttpSession session
    ) throws ContainerException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        int pageSize = 20;
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        return jobRunLogRepository.findByJobId(jobId, pageable);
    }


    @GetMapping("/running_details/{run_id}")
    public String getRunningDetails(
            HttpServletResponse response,
            @PathVariable(name = "project_id") int projectId,
            @PathVariable(name = "job_id") int jobId,
            @PathVariable(name = "run_id") int runId,
            HttpSession session
    ) throws ContainerException, IOException, InterruptedException, KeeperException {
        ProjectEntity projectEntity = ControllerUtil.findProjectOnExist(projectRepository, projectId);
        JobEntity jobEntity = ControllerUtil.findJobOnExist(jobRepository, jobId);
        if (!projectService.checkAuthorization(projectEntity, session)) {
            throw new UnauthorizedException();
        }
        JobRunLogEntity jobRunLogEntity = ControllerUtil.findJobRunOnExist(jobRunLogRepository, runId);
        StringBuffer stringBuffer = null;
        if (jobRunLogEntity.getRunningHost() == null || jobRunLogEntity.getRunningHost().equals(hostNameProvider.getHostName())) {
            stringBuffer = jobRunService.getRunningDetails(jobRunLogEntity);
        } else {
            stringBuffer = zkJobLogProducer.getLogsAwait(jobRunLogEntity, 5);
        }
        if (stringBuffer == null) {
            return "no log found for:" + jobRunLogEntity.getRunId();
        }
        return stringBuffer.toString().replace("\n", "</br>");
    }


}
