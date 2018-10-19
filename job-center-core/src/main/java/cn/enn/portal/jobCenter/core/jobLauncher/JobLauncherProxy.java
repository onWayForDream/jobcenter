package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.core.SupportedRuntime;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class JobLauncherProxy implements JobLauncher {

    @Autowired
    private JavaJobLauncher javaJobLauncher;

    @Autowired
    private LinuxJobLauncher linuxJobLauncher;

    @Autowired
    private SpringBootJobLauncher springBootJobLauncher;

    private JobLauncher getTargetLauncher(String supportedRuntimeString) {
        if (supportedRuntimeString == null || supportedRuntimeString.isEmpty()) {
            return this.javaJobLauncher;
        }
        SupportedRuntime supportedRuntime = SupportedRuntime.valueOf(supportedRuntimeString);
        switch (supportedRuntime) {
            case LinuxShell:
            case Python2:
            case Python3:
                return this.linuxJobLauncher;
            case SpringBoot:
                return this.springBootJobLauncher;
            default:
                return this.javaJobLauncher;
        }
    }


    @Override
    public PJob loadPJob(ProjectEntity project, JobEntity job) throws Exception {
        return getTargetLauncher(job.getRuntimeType()).loadPJob(project, job);
    }

    @Override
    public PJobResolver loadPJobResolver(ProjectEntity project, JobEntity job) throws Exception {
        return getTargetLauncher(job.getRuntimeType()).loadPJobResolver(project, job);
    }

    @Override
    public Properties loadConfigProperties(ProjectEntity project, JobEntity job) throws Exception {
        return getTargetLauncher(job.getRuntimeType()).loadConfigProperties(project, job);
    }

}
