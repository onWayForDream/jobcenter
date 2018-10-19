package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.jobRunner.LinuxShellJobRunner;
import org.springframework.stereotype.Component;

@Component
public class LinuxJobLauncher extends JobLauncherBase {

    @Override
    public PJob loadPJob(ProjectEntity project, JobEntity job) throws Exception {
        return new LinuxShellJobRunner();
    }

    @Override
    public PJobResolver loadPJobResolver(ProjectEntity project, JobEntity job) throws Exception {
        throw new UnsupportedOperationException("linux job not support resolver!");
    }
    
}
