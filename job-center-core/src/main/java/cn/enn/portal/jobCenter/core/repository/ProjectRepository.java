package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {

    public Page<ProjectEntity> findByOwner(String owner, Pageable pageable);

    public Page<ProjectEntity> findByNameContaining(String name, Pageable pageable);

    public Page<ProjectEntity> findByOwnerAndNameContaining(String owner, String name, Pageable pageable);

    @Query(value = "select project_id from job_run_log where id in (select max(id) as last_id from job_run_log where job_status_code != 'RUNNING' group by job_id) and job_status_code = 'FAILED'",
            nativeQuery = true)
    public List<Integer> findExceptionProject();


}
