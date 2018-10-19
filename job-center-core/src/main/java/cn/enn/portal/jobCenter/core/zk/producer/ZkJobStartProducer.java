package cn.enn.portal.jobCenter.core.zk.producer;

import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.zk.ZkLock;
import org.apache.zookeeper.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ZkJobStartProducer extends ZkJobProducerBase {

    @Autowired
    private ZkLock zkLock;

    public void startJob(JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator) throws Exception {
        startJob(jobEntity, scheduleActionType, operator, null);
    }

    /**
     * 启动一个job，运行的节点由系统自动分配
     *
     * @param jobEntity
     * @param scheduleActionType
     * @param operator
     * @param overrideStartTime
     * @throws Exception
     */
    public void startJob(JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator, Long overrideStartTime) throws Exception {
        String idleHost = findIdleHost();
        startJobAt(jobEntity, scheduleActionType, operator, overrideStartTime, idleHost);
    }

    /**
     * 启动一个job，并在某个节点上运行
     *
     * @param jobEntity
     * @param scheduleActionType
     * @param operator
     * @param overrideStartTime
     * @param hostName
     * @throws Exception
     */
    public void startJobAt(JobEntity jobEntity, ScheduleActionType scheduleActionType, String operator, Long overrideStartTime, String hostName) throws Exception {
        if (hostName == null || hostName.isEmpty()) {
            throw new RuntimeException("no host specified");
        }
        String znode = getZnodePath();
        updateRunningJobCount(1, hostName);

        if (hostName.equals(hostNameProvider.getHostName())) {
            Optional<Date> optionalDate = Optional.empty();
            if (overrideStartTime != null) {
                optionalDate = Optional.of(new Date(overrideStartTime));
            }
            super.jobScheduleService.prepareToRunJob(jobEntity, scheduleActionType, operator, optionalDate);
            logger.info("start job directly without zookeeper, job name = {}", jobEntity.getJobName());
        } else {
            //znode key : SEQUENTIAL
            //znode value : {"jobId":282,"hostName":"host1","ScheduleActionType":"USER_SCHEDULE","operator":"user1","startTime":1212121212L}
            JSONObject contentJson = new JSONObject();
            contentJson.put("jobId", jobEntity.getId());
            contentJson.put("hostName", hostName);
            contentJson.put("ScheduleActionType", scheduleActionType.toString());
            contentJson.put("operator", operator);
            if (overrideStartTime != null) {
                contentJson.put("startTime", overrideStartTime);
            }
            String sequentialNode = zooKeeper.create(znode, contentJson.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("produce start job at {},content = {}", sequentialNode, contentJson.toString());
        }
    }

    /**
     * 排除掉不能运行的节点，返回运行任务数最少的节点
     *
     * @return
     */
    private String findIdleHost() {
        List<String> ignoreHostList = getIgnoreHost();
        try {
            List<String> runningHosts = zooKeeper.getChildren(zookeeperClient.getClientPath(), false);
            for (String host : ignoreHostList) {
                runningHosts.remove(host);
            }
            String idleHost = "";
            int minRunningJobs = Integer.MAX_VALUE;
            for (String hostName : runningHosts) {
                int _runningJobs = getRunningJobsOfHost(hostName);
                logger.debug("{} has {} jobs", hostName, _runningJobs);
                if (_runningJobs < minRunningJobs) {
                    minRunningJobs = _runningJobs;
                    idleHost = hostName;
                }
            }
            return idleHost;
        } catch (Exception e) {
            logger.error("findIdleHost error", e);
            return null;
        }
    }


    private List<String> getIgnoreHost() {
        // 发现哪些主机不能运行新的任务。
        // 目前来看，维护模式下的主机不能运行新的任务
        try {
            return super.zooKeeper.getChildren(zookeeperClient.getMaintenancePath(), false);
        } catch (Exception ex) {
            logger.error("get maintenance hosts failed", ex);
            return new LinkedList<>();
        }
    }

    @Override
    protected String getZnodePath() {
        return zookeeperClient.getStartJobQueuePath() + "/item-";
    }


}
