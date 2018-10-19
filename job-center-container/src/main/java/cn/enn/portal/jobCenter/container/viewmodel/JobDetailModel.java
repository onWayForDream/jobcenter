package cn.enn.portal.jobCenter.container.viewmodel;

import cn.enn.portal.jobCenter.core.JobRunStatus;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;

public class JobDetailModel extends JobListItemViewModel {
    public JobDetailModel(JobEntity jobEntity, JobRunLogRepository jobRunLogRepository) {
        this(jobEntity, 0, jobRunLogRepository);
    }

    public JobDetailModel(JobEntity jobEntity, int subJobCount, JobRunLogRepository jobRunLogRepository) {
        super(jobEntity, subJobCount);
        this.runTimesTotal = jobRunLogRepository.countByJobId(jobEntity.getId());
        this.runTimesSuccess = jobRunLogRepository.countByJobIdAndJobStatusCode(jobEntity.getId(),JobRunStatus.DONE.toString());
    }

    private int runTimesTotal;
    private int runTimesSuccess;


    public int getRunTimesTotal() {
        return runTimesTotal;
    }

    public void setRunTimesTotal(int runTimesTotal) {
        this.runTimesTotal = runTimesTotal;
    }

    public int getRunTimesSuccess() {
        return runTimesSuccess;
    }

    public void setRunTimesSuccess(int runTimesSuccess) {
        this.runTimesSuccess = runTimesSuccess;
    }

}
