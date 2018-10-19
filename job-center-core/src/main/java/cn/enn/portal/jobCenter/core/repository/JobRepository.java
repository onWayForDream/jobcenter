package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;


public interface JobRepository extends JpaRepository<JobEntity, Integer> {

    @Query("select j from job j where j.projectId = ?1 and (resolver_id = 0 or resolver_id is null)")
    Page<JobEntity> findByProjectId(int projectId, Pageable pageable);

    @Query("select j from job j where j.projectId = ?1 and jobName like %?2% and (resolver_id = 0 or resolver_id is null)")
    Page<JobEntity> findByProjectIdAndJobName(int projectId, String jobName, Pageable pageable);

    @Query("select j from job j where j.projectId = ?1 and status =?2 and (resolver_id = 0 or resolver_id is null)")
    Page<JobEntity> findByProjectIdAndStatus(int projectId, String status, Pageable pageable);

    @Query("select j from job j where j.projectId = ?1 and status =?2 and jobName like %?3% and (resolver_id = 0 or resolver_id is null)")
    Page<JobEntity> findByProjectIdAndStatusAndJobName(int projectId, String status, String jobName, Pageable pageable);

    @Query(nativeQuery = true, value = "select * from job where resolver_id = ?1")
    List<JobEntity> findByResolverId(int resolverId);

    @Query(nativeQuery = true, value = "select * from job where resolver_id = ?1 and disabled = ?2)")
    List<JobEntity> findByResolverIdAndDisabled(int resolverId, int disabled);

    Page<JobEntity> findByResolverId(int resolverId, Pageable pageable);

    @Query(nativeQuery = true, value = "select * from job where project_id = ?1 and job_name = ?2 and disabled = 1")
    JobEntity findDisabledJob(int projectId, String jobName);

    int countByResolverId(int resolverId);

    boolean existsByProjectIdAndJobName(int projectId, String jobName);

    @Query(value = "select COUNT(*) from job where project_id = ?1 and job_name = ?2 and id != ?3", nativeQuery = true)
    int countByExistJob(int projectId, String jobName, int id);

    @Transactional
    void deleteByIdIn(Collection<Integer> idList);

    @Query(value = "select * from job where status != 'DONE' and running_host = ?1 order by resolver_id desc, id", nativeQuery = true)
    List<JobEntity> findJobsToRestore(String hostName);

    @Modifying
    @Transactional
    @Query(value = "update job set status = 'DONE' ", nativeQuery = true)
    void resetJobStatus();


}
