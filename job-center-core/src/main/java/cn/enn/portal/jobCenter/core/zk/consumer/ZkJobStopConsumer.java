package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ZkJobStopConsumer extends ZkJobConsumerBase {
    @Override
    protected String getJobQueuePath() {
        return super.zookeeperClient.getStopJobQueuePath();
    }

    @Override
    protected void processJobItem(String znode, JSONObject contentJson) throws Exception {
        int jobId = contentJson.getInt("jobId");

        JobEntity jobEntity = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("can not find job with id:" + jobId));
        ProjectEntity projectEntity = projectRepository.findById(jobEntity.getProjectId())
                .orElseThrow(() -> new RuntimeException("can not find project with id:" + jobEntity.getProjectId()));

        ScheduleActionType scheduleActionType = ScheduleActionType.valueOf(contentJson.getString("ScheduleActionType"));
        String operator = contentJson.getString("operator");
        boolean recursive = contentJson.getBoolean("recursive");

        logger.info("begin to stop job from zk queue:{},content:{}", znode, contentJson.toString());
        super.jobScheduleService.shutdownJob(projectEntity, jobEntity, scheduleActionType, operator, recursive);
        logger.info("stop job from zk queue success:{}", znode);
    }
}
