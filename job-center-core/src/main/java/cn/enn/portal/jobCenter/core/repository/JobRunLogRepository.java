package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.entity.tmpEntity.RunningJobsOfHosts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface JobRunLogRepository extends JpaRepository<JobRunLogEntity, Integer> {

    @Query(value = "select * from job_run_log where id in ( select max(id) from job_run_log where project_id = ?1 and job_id in ?2  group by job_id)",
            nativeQuery = true)
    List<JobRunLogEntity> findLatestStatus(int projectId, Collection<Integer> jobIdList);

    int countByJobId(int jobId);

    int countByJobIdAndJobStatusCode(int jobId, String jobStatusCode);

    Page<JobRunLogEntity> findByJobId(int jobId, Pageable pageable);

    @Query(value = "select running_host AS host, COUNT(*) AS job_count from job where status != 'DONE'  group by running_host", nativeQuery = true)
    List<RunningJobsOfHosts> getRunningJobOfHosts();
}
