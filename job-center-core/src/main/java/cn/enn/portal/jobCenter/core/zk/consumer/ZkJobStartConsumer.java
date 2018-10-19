package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class ZkJobStartConsumer extends ZkJobConsumerBase {

    @Override
    protected String getJobQueuePath() {
        return super.zookeeperClient.getStartJobQueuePath();
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
        Optional<Date> date = Optional.empty();
        if (contentJson.has("startTime")) {
            long startTimeStamp = contentJson.getLong("startTime");
            if (startTimeStamp > 0) {
                date = Optional.of(new Date(startTimeStamp));
            }
        }

        logger.info("begin to start job from zk queue:{},content:{}", znode, contentJson.toString());
        jobScheduleService.prepareToRunJob(projectEntity, jobEntity, scheduleActionType, operator, date);
        logger.info("start job from zk queue success:{}", znode);
    }

}
