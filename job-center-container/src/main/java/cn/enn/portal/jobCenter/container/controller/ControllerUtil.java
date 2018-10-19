package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;
import java.util.Optional;

public class ControllerUtil {
    public static ProjectEntity findProjectOnExist(ProjectRepository projectRepository, int projectId) throws ContainerException {
        Optional<ProjectEntity> projectEntity = projectRepository.findById(projectId);
        if (!projectEntity.isPresent()) {
            throw new ContainerException(MessageFormat.format("project_id {0} not found", projectId), HttpStatus.NOT_FOUND);
        }
        return projectEntity.get();
    }

    public static JobEntity findJobOnExist(JobRepository jobRepository, int jobId) throws ContainerException {
        Optional<JobEntity> jobEntity = jobRepository.findById(jobId);
        if (!jobEntity.isPresent()) {
            throw new ContainerException(MessageFormat.format("job_id {0} not found", jobId), HttpStatus.NOT_FOUND);
        }
        return jobEntity.get();
    }

    public static JobRunLogEntity findJobRunOnExist(JobRunLogRepository jobRunLogRepository, int runId) throws ContainerException {
        Optional<JobRunLogEntity> jobRunLogEntity = jobRunLogRepository.findById(runId);
        if (!jobRunLogEntity.isPresent()) {
            throw new ContainerException(MessageFormat.format("run_id {0} not found", runId), HttpStatus.NOT_FOUND);
        }
        return jobRunLogEntity.get();
    }

}
