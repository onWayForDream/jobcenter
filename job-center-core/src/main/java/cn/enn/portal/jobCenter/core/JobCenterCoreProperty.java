package cn.enn.portal.jobCenter.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "jobcenter.core")
public class JobCenterCoreProperty {

    private String jobJarRoot;

    public String getJobJarRoot() {
        return jobJarRoot;
    }

    public void setJobJarRoot(String jobJarRoot) {
        this.jobJarRoot = jobJarRoot;
    }

    public File getProjectFolder(int projectId) {
        String directory = jobJarRoot + "/" + projectId;
        return new File(directory);
    }
}
