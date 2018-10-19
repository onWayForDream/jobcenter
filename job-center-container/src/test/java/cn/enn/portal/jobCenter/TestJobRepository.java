package cn.enn.portal.jobCenter;

import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJobRepository {

    @Autowired
    JobRepository jobRepository;

    @Test
    public void testFindByProjectId() {
        Page<JobEntity> jobs = jobRepository.findByProjectId(1, PageRequest.of(0, 10));
//        Assert.assertTrue(jobs.getContent().size() > 0);
        for (JobEntity jobEntity : jobs.getContent()) {
            System.out.println("-------------testFindByProjectId----------------:" + jobEntity.getJobName());
        }
    }

    @Test
    public void testFindByProjectIdAndJobName() {
        Page<JobEntity> jobs = jobRepository.findByProjectIdAndJobName(1, "test", PageRequest.of(0, 10));
//        Assert.assertTrue(jobs.getContent().size() > 0);
        for (JobEntity jobEntity : jobs.getContent()) {
            System.out.println("-------------findByProjectIdAndJobName----------------:" + jobEntity.getJobName());
        }
    }

    @Test
    public void testFindByProjectIdAndStatus() {
        Page<JobEntity> jobs = jobRepository.findByProjectIdAndStatus(1, "test", PageRequest.of(0, 10));
//        Assert.assertTrue(jobs.getContent().size() == 0);
        for (JobEntity jobEntity : jobs.getContent()) {
            System.out.println("-------------findByProjectIdAndStatus----------------:" + jobEntity.getJobName());
        }
    }

}
