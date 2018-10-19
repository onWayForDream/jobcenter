package cn.enn.portal.jobCenter.core.zk.producer;

import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import org.apache.zookeeper.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ZkJobStopProducer extends ZkJobProducerBase {

    @Autowired
    private JobRepository jobRepository;

    public void stopJob(JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator, boolean recursive) throws KeeperException, InterruptedException {
        jobEntity = refreshJob(jobEntity);
        String runningHost = jobEntity.getRunningHost();
        if (runningHost.equals(super.hostNameProvider.getHostName())) {
            super.jobScheduleService.shutdownJob(jobEntity, scheduleActionType, operator, recursive);
            logger.info("shutdown job directly without zookeeper, job name = {}", jobEntity.getJobName());
        } else {
            String znode = getZnodePath();
            //znode key : SEQUENTIAL
            //znode value : {"jobId":282,"hostName":"host1","ScheduleActionType":"USER_SCHEDULE","operator":"user1","startTime":1212121212L}
            JSONObject contentJson = new JSONObject();
            contentJson.put("jobId", jobEntity.getId());
            contentJson.put("hostName", runningHost);
            contentJson.put("ScheduleActionType", scheduleActionType.toString());
            contentJson.put("operator", operator);
            contentJson.put("recursive", recursive);
            String sequentialNode = zooKeeper.create(znode, contentJson.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("produce stop job at {},content = {}", sequentialNode, contentJson.toString());
        }
        // decrease running job count
        updateRunningJobCount(-1, runningHost);
    }

    private JobEntity refreshJob(JobEntity job) {
        Optional<JobEntity> optionalJob = jobRepository.findById(job.getId());
        if (optionalJob.isPresent()) {
            return optionalJob.get();
        }
        return job;
    }

    @Override
    protected String getZnodePath() {
        return zookeeperClient.getStopJobQueuePath() + "/item-";
    }


}
