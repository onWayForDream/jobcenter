package cn.enn.portal.jobCenter.core.jobRunner;

import cn.enn.portal.jobCenter.PJobMetadata;
import cn.enn.portal.jobCenter.core.JobcenterLoggerProxy;
import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import org.apache.zookeeper.KeeperException;

import java.text.MessageFormat;
import java.util.List;

public class RepeatedPJobResolverRunner extends CommonRepeatedRunner {


    @Override
    public String runUserJob() throws Exception {

        int newJobCount = 0, disableJobCount = 0, updateJobCount = 0, enableJobCount = 0;

        // resolve schedulers
        List<PJobMetadata> newPJobMetadataList = resolverInstance.resolvePJobs(jobParam,
                new JobcenterLoggerProxy(logger, runId), configProperties);
        List<JobEntity> existJobs = jobRepository.findByResolverId(jobEntity.getId());
        for (PJobMetadata pjobMetadata : newPJobMetadataList) {
            JobEntity existItem = findExistJob(pjobMetadata.getJobName(), existJobs);
            if (existItem == null) {
                // no exist PJobMetadata found
                // add PJobMetadata, and prepare to run it
                JobEntity newJob = createJob(pjobMetadata);
//                jobScheduleService.prepareToRunJob(projectEntity, newJob, ScheduleActionType.RESOLVER_CREATE, jobEntity.getOwner());
                runJobByZk(newJob);
                logger.debug("{}no exist job found, create job, and prepare to run it:{}", runIdStr, newJob.getJobName());
                newJobCount++;

            } else if (existItem.getIdentifierCode() != pjobMetadata.getIdentifierCode()
                    || !existItem.getExecuteClass().equals(pjobMetadata.getExecuteClass())
                    || existItem.getDisabled() == 1) {
                logger.debug("{}exist job found, but identifierCode (or executeClass, or disabled) changed:{}", runIdStr, existItem.getJobName());
                // exist PJobMetadata found, but rule changed
                // stop PJobMetadata, recreate it and rerun it
                if (jobScheduleService.isJobRunning(existItem)) {
//                    jobScheduleService.shutdownJob(projectEntity, existItem, ScheduleActionType.RESOLVER_UPDATE, jobEntity.getOwner());
                    stopJobByZk(existItem);
                    logger.debug("{}shutdown job:{}", runIdStr, existItem.getJobName());
                }
                JobEntity updatedJob = jobScheduleService.updateJobMetadata(pjobMetadata, existItem.getId());
//                jobScheduleService.prepareToRunJob(projectEntity, newJob, ScheduleActionType.RESOLVER_CREATE, jobEntity.getOwner());
                runJobByZk(updatedJob);
                logger.debug("{}recreate it and rerun PJobMetadata:{}", runIdStr, existItem.getJobName());
                if (existItem.getDisabled() == 0)
                    updateJobCount++;
                else
                    enableJobCount++;
            } else {
                // exist PJobMetadata found, and rule not changed
                // start it if the PJobMetadata is not running
                logger.debug("{}exist PJobMetadata found, and rule not changed:{}", runIdStr, existItem.getJobName());
                if (!jobScheduleService.isJobRunning(existItem)) {
//                    jobScheduleService.prepareToRunJob(projectEntity, existItem, ScheduleActionType.RESOLVER_CREATE, jobEntity.getOwner());
                    runJobByZk(existItem);
                    logger.debug("{}run PJobMetadata:{}", runIdStr, existItem.getJobName());
                }
            }
        }
        JobEntity jobToDelete = null;
        while ((jobToDelete = findDeletedJob(newPJobMetadataList, existJobs)) != null) {
            existJobs.remove(jobToDelete);
            if (jobToDelete.getDisabled() == 1) {
                continue;
            }
            // stop scheduler, and delete it
            if (jobScheduleService.isJobRunning(jobToDelete)) {
//                jobScheduleService.shutdownJob(projectEntity, schedulerToDelete, ScheduleActionType.RESOLVER_DELETE, jobEntity.getOwner());
                stopJobByZk(jobToDelete);
                logger.debug("{}shutdown job {}", runIdStr, jobToDelete.getJobName());
            }
            jobScheduleService.disableJobById(jobToDelete.getId());
            logger.debug("{}delete job {}", runIdStr, jobToDelete.getJobName());
            disableJobCount++;
        }
        return MessageFormat.format("new:{0}, update:{1}, disable:{2}, enable:{3}",
                newJobCount, updateJobCount, disableJobCount, enableJobCount);
    }

    private JobEntity findExistJob(String jobName, List<JobEntity> existJobs) {
        for (JobEntity job : existJobs) {
            if (job.getJobName().equals(jobName)) {
                return job;
            }
        }
        return null;
    }

    private JobEntity findDeletedJob(List<PJobMetadata> newPJobMetadataList, List<JobEntity> existJobs) {
        for (JobEntity _existItem : existJobs) {
            boolean _isOccured = false;
            for (PJobMetadata _newItem : newPJobMetadataList) {
                if (_existItem.getJobName().equals(_newItem.getJobName())) {
                    _isOccured = true;
                    break;
                }
            }
            if (!_isOccured) {
                return _existItem;
            }
        }
        return null;
    }

    private JobEntity createJob(PJobMetadata jobMetadata) {
        if (jobRepository.existsByProjectIdAndJobName(projectEntity.getId(), jobMetadata.getJobName())) {
            throw new IllegalArgumentException(MessageFormat.format("{1}job name [{0}] already exist in current project",
                    jobMetadata.getJobName(), runIdStr));
        }
        JobEntity newJob = new JobEntity();
        newJob.inflateByScheduler(jobMetadata);
        newJob.setProjectId(projectEntity.getId());
        newJob.setIsResolverJob(0);
        newJob.setOwner(jobEntity.getOwner());
        newJob.setResolverId(jobEntity.getId());
        newJob.setRuntimeType(jobMetadata.getRuntimeTime());

        return jobRepository.save(newJob);
    }

    private void runJobByZk(JobEntity jobEntity) throws Exception {
        zkJobStartProducer.startJob(jobEntity, ScheduleActionType.RESOLVER_CREATE, jobEntity.getOwner());
    }

    private void stopJobByZk(JobEntity jobEntity) throws InterruptedException, KeeperException {
        zkJobStopProducer.stopJob(jobEntity, ScheduleActionType.RESOLVER_DELETE, jobEntity.getOwner(), false);
    }


}
