package cn.enn.portal.jobCenter.core.zk;

import cn.enn.portal.jobCenter.ScheduleType;
import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStartProducer;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShutdownWatcher implements Watcher {

    @Autowired
    private ZookeeperClient zookeeperClient;

    @Autowired
    private HostNameProvider hostNameProvider;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ZkJobStartProducer zkJobStartProducer;

    @Autowired
    private JobScheduleService jobScheduleService;

    private ZooKeeper zooKeeper;

    private List<String> childrenList = null;

    private Logger logger = LoggerFactory.getLogger(ShutdownWatcher.class);

    public void startWatcher() throws IOException, InterruptedException, KeeperException {
        zooKeeper = zookeeperClient.connect();
        childrenList = zooKeeper.getChildren(zookeeperClient.getClientPath(), this);
        printChildrenList();
    }


    @Override
    public void process(WatchedEvent event) {
        logger.info("event type is " + event.getType().name());

        while (restoreFailureHost(event)) {

        }

        try {
            childrenList = zooKeeper.getChildren(zookeeperClient.getClientPath(), this);
        } catch (Exception e) {
            logger.error("set another watcher failed", e);
        }
        printChildrenList();
    }


    private boolean restoreFailureHost(WatchedEvent event) {
        List<String> _list = null;
        try {
            _list = zooKeeper.getChildren(zookeeperClient.getClientPath(), false);
        } catch (Exception e) {
            logger.error("get clients error", e);
            return false;
        }
        if (event.getType().equals(Event.EventType.NodeChildrenChanged) && childrenList.size() > _list.size()) {
            List<String> failureHosts = getFailureHosts(this.childrenList, _list);
            // 现在，failureHosts中就是down掉的机器了
            for (String hostname : failureHosts) {
                try {
                    restoreRunningJobsOfHost(hostname);
                    // 恢复完成之后，将hostname从this.childrenList中移除，以保证this.childrenList是假设的正在运行的节点
                    this.childrenList.remove(hostname);
                } catch (Exception ex) {
                    logger.error("restore jobs of failed:" + hostname, ex);
                }
            }
            return failureHosts.size() > 0;
        } else {
            this.childrenList = _list;
            return false;
        }
    }

    private List<String> getFailureHosts(List<String> originList, List<String> currentList) {
        List<String> resultList = new LinkedList<>();
        for (String host : originList) {
            if (!currentList.contains(host)) {
                resultList.add(host);
            }
        }
        return resultList;
    }

    /**
     * 当一台主机挂掉后，由另一台主机去恢复那些因为jobcenter进程停止而终止运行的任务。
     * 恢复逻辑如下：
     * --   如果超出了有效期，则修改job状态为停止
     * --   //如果没有到有效期，则加入future task队列（直接调用prepareToRunScheduler即可）
     * --   如果有开始有效期，并且已经过了开始有效期，并且调度方式是INTERVAL，则将下次运行时间设置到下一个INTERVAL触发的时候（FUTURE TASK）
     * --   其余情况，直接调用prepareToRunScheduler
     */
    public void restoreRunningJobsOfHost(String hostname) {
        logger.info("begin to restore running jobs in:{}", hostname);
        // 将该主机上由job resolver生成的job的运行状态修改为DONE
//        jobRepository.resetRunningResolvedJobs(hostname);
        List<JobEntity> jobList = jobRepository.findJobsToRestore(hostname);
        for (JobEntity job : jobList) {
            // 已过期
            if (job.getValidityTo() != null && job.getValidityTo().compareTo(new Date()) < 0) {
                continue;
            }
            Optional<ProjectEntity> projectEntity = projectRepository.findById(job.getProjectId());
            if (!projectEntity.isPresent()) {
                continue;
            }
            try {
                Date overrideRestartTime = jobScheduleService.getOverrideRestartTime(job);
                if (overrideRestartTime != null) {
                    zkJobStartProducer.startJob(job, ScheduleActionType.SYSTEM_STARTUP_SCHEDULE, "system", overrideRestartTime.getTime());
                } else {
                    zkJobStartProducer.startJob(job, ScheduleActionType.SYSTEM_STARTUP_SCHEDULE, "system");
                }
                logger.info("restore job async,job name = {}", job.getJobName());
            } catch (Exception ex) {
                logger.error("restore job error, jobId = " + job.getId(), ex);
            }
        }
    }


    private void printChildrenList() {
        logger.info("print children node of " + zookeeperClient.getClientPath());
        for (String hostname : childrenList) {
            logger.info("--------------found:" + hostname);
        }
        logger.info("print children end");
    }

}
