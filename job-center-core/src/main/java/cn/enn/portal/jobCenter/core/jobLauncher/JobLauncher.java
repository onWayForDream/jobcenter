package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;

import java.util.Properties;

public interface JobLauncher {

    PJob loadPJob(ProjectEntity project, JobEntity job) throws Exception;

    PJobResolver loadPJobResolver(ProjectEntity project, JobEntity job) throws Exception;

    Properties loadConfigProperties(ProjectEntity project, JobEntity job) throws Exception;

}
