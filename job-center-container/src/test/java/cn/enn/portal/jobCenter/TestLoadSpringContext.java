package cn.enn.portal.jobCenter;

import cn.enn.portal.jobCenter.core.JobcenterLoggerProxy;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.jobLauncher.JobLauncherProxy;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestLoadSpringContext {

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    JobLauncherProxy jobLauncherProxy;

    @Autowired
    JobRepository jobRepository;

    Logger logger = LoggerFactory.getLogger(TestLoadSpringContext.class);

    @Test
    public void testRunBeanJob() throws Exception {
        ProjectEntity project = projectRepository.findById(2).get();
        JobEntity job = jobRepository.findById(404).get();
        PJob pJob = jobLauncherProxy.loadPJob(project, job);
        String result = pJob.executeJob("", new JobcenterLoggerProxy(logger, UUID.randomUUID()), null);
        System.out.println("executeJob result is : " + result);

        pJob = jobLauncherProxy.loadPJob(project, job);
        result = pJob.executeJob("", new JobcenterLoggerProxy(logger, UUID.randomUUID()), null);
        System.out.println("executeJob result is : " + result);
    }
}
