package cn.enn.portal.jobCenter.core.repository;

import cn.enn.portal.jobCenter.core.entity.JobScheduleLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobScheduleLogRepository extends JpaRepository<JobScheduleLogEntity, Integer> {
}
