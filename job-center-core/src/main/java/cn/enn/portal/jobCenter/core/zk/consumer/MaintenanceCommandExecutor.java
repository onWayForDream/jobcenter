package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.FutureSchedulerManager;
import cn.enn.portal.jobCenter.core.ScheduleActionType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.jobRunner.PJobRunLock;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.ZkLock;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStartProducer;
import cn.enn.portal.jobCenter.core.zk.producer.ZkJobStopProducer;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Component
public class MaintenanceCommandExecutor {

    public static final String MAINTENANCE_COMMAND_PREFIX = "maintenance-";
    private Logger logger = LoggerFactory.getLogger(MaintenanceCommandExecutor.class);

    /**
     * 为true时，不能运行任何任务
     */
    public volatile boolean GLOBAL_SHUTDOWN_FLAG = false;

    @Autowired
    private ZkLock zkLock;

    @Autowired
    private HostNameProvider hostNameProvider;

    @Autowired
    private ZookeeperClient zookeeperClient;

    @Autowired
    private FutureSchedulerManager futureSchedulerManager;

    @Autowired
    private JobScheduleService jobScheduleService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ZkJobStartProducer zkJobStartProducer;

    @Autowired
    private ZkJobStopProducer zkJobStopProducer;

    public void shutdown() {
        String hostname = hostNameProvider.getHostName();
        ZooKeeper zk = zookeeperClient.connect();
        String commandName = MAINTENANCE_COMMAND_PREFIX + hostname;
        String commandDesc = MessageFormat.format("正在对节点[{0}]进行下线处理", hostname);
        if (!zkLock.lock(commandName, commandDesc)) {
            return;
        }
        try {

            // 如何优雅地让一台服务器下线？
            // 0、设置一个全局开关，不再调度任何新任务
            // 1、转移将要运行的job（FutureTask）
            // 2、转移状态为RUNNING但并没有真正正在运行（等待被触发）的job
            // 3、等待正在调度的任务的本次运行结束，将其转移
            // 4、关闭全局开关

            // 0、设置一个全局开关，不再调度任何新任务(调用zkLock时加了一个全局锁，其它节点也不会向当前节点分配任务了，直到释放这个锁）

            // 1、转移将要运行的job（FutureTask）
            for (String key : FutureSchedulerManager.futureRunTasks.keySet()) {
                try {
                    String[] ids = futureSchedulerManager.resolveKey(key);
                    int jobId = Integer.parseInt(ids[1]);
                    Optional<JobEntity> jobEntityOptional = jobRepository.findById(jobId);
                    if (jobEntityOptional.isPresent()) {
                        try {
                            rerunJob(jobEntityOptional.get());
                            logger.info("success to rerun job from future task :{}", jobEntityOptional.get().getJobName());
                        } catch (Exception ex) {
                            logger.error("fail to rerun job from future task :" + jobEntityOptional.get().getJobName(), ex);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("cancel future job run error", ex);
                }
            }

            // 2、转移状态为RUNNING但并没有真正正在运行（等待被触发）的job
            List<JobEntity> runningJobs = jobScheduleService.getAllRunningJobs();
            for (JobEntity job : runningJobs) {
                // 确定job不是active状态
                if (!PJobRunLock.isJobActive(job.getId())) {
                    try {
                        rerunJob(job);
                        logger.info("success to rerun job from running job (but not active) :{}", job.getJobName());
                    } catch (Exception ex) {
                        logger.error("fail to rerun job from running job (but not active) :" + job.getJobName(), ex);
                    }
                }
            }
            // 第3步在CommonRepeatedRunner的execute方法最后面实现，它会拦截所有结束的job，一旦结束，立刻shutdown job
            GLOBAL_SHUTDOWN_FLAG = true;
            // 等待所有任务全部结束，，然后在finally中关闭全局开关
            if (jobScheduleService.getAllRunningJobs().size() > 0) {
                logger.info("begin wait active jobs shutdown");
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<Boolean> callable = executorService.submit(new WaitingThread());
                callable.get(30, TimeUnit.MINUTES);
                logger.info("all active jobs shutdown complete");
            }

        } catch (Exception ex) {
            logger.error("shutdown failed", ex);
        } finally {
            GLOBAL_SHUTDOWN_FLAG = false;
            zkLock.unlock(commandName);
        }
    }

    public void rerunJob(JobEntity job) throws Exception {
        Date overrideRestartTime = jobScheduleService.getOverrideRestartTime(job);
        zkJobStopProducer.stopJob(job, ScheduleActionType.SYSTEM_SHUTDOWN, "administrator", false);
        if (overrideRestartTime != null) {
            zkJobStartProducer.startJob(job, ScheduleActionType.SYSTEM_SCHEDULE, "administrator", overrideRestartTime.getTime());
        } else {
            zkJobStartProducer.startJob(job, ScheduleActionType.SYSTEM_SCHEDULE, "administrator");
        }
    }

    class WaitingThread implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            while (true) {
                int runningJobCount = jobScheduleService.getAllRunningJobs().size();
                logger.info("running job count:{}", runningJobCount);
                if (runningJobCount == 0) {
                    break;
                } else {
                    Thread.sleep(1000);
                }
            }
            return true;
        }
    }
}
