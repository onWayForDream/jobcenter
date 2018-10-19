package cn.enn.portal.jobCenter.core;

import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.*;

@Component
public class FutureSchedulerManager {

    public static ConcurrentHashMap<String, ScheduledFuture> futureRunTasks = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ScheduledFuture> futureShutdownTasks = new ConcurrentHashMap<>();

    private static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(5);

    private static Logger logger = LoggerFactory.getLogger(FutureSchedulerManager.class);

    public void runInFuture(Date futureDate,
                            JobScheduleService jobScheduleService,
                            ProjectEntity projectEntity,
                            JobEntity jobEntity,
                            ScheduleActionType scheduleActionType,
                            String operator) {

        long delaySeconds = getDelaySeconds(futureDate);
        String key = getKey(projectEntity.getId(), jobEntity.getId());
        if (futureRunTasks.contains(key)) {
            throw new RuntimeException("job is already in cached, do not cached again:" + jobEntity.getJobName());
        }
        ScheduledFuture<?> scheduledFuture = threadPool.schedule(() -> {
                    futureRunTasks.remove(key);
                    try {
                        logger.info("future job runs: {}", jobEntity.getJobName());
                        jobScheduleService.runJob(projectEntity, jobEntity, scheduleActionType, operator);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                delaySeconds, TimeUnit.SECONDS);
        futureRunTasks.put(key, scheduledFuture);
    }

    public void shutdownInFuture(Date futureDate,
                                 JobScheduleService jobScheduleService,
                                 ProjectEntity projectEntity,
                                 JobEntity jobEntity,
                                 ScheduleActionType scheduleActionType,
                                 String operator) {
        long delaySeconds = getDelaySeconds(futureDate);
        String key = getKey(projectEntity.getId(), jobEntity.getId());
        ScheduledFuture<?> scheduledFuture = threadPool.schedule(() -> {
                    logger.info("future job shutdown: {} ", jobEntity.getJobName());
                    jobScheduleService.shutdownJob(projectEntity, jobEntity, scheduleActionType, operator, true);
                    futureShutdownTasks.remove(key);
                },
                delaySeconds, TimeUnit.SECONDS);
        futureShutdownTasks.put(key, scheduledFuture);
    }

    public boolean isRunInfuture(int projectId, int jobId) {
        String key = getKey(projectId, jobId);
        return futureRunTasks.contains(key);
    }

    public boolean isShutdownInFuture(int projectId, int jobId) {
        String key = getKey(projectId, jobId);
        return futureShutdownTasks.contains(key);
    }

    public void cancelRun(int projectId, int jobId) {
        String key = getKey(projectId, jobId);
        ScheduledFuture scheduledFuture = futureRunTasks.remove(key);
        if (scheduledFuture != null && !scheduledFuture.isCancelled())
            scheduledFuture.cancel(false);
    }

    public void cancelShutdown(int projectId, int jobId) {
        String key = getKey(projectId, jobId);
        ScheduledFuture scheduledFuture = futureShutdownTasks.remove(key);
        if (scheduledFuture != null && !scheduledFuture.isCancelled())
            scheduledFuture.cancel(false);
    }

    public long getDelaySeconds(Date futureDate) {
        long diff = futureDate.getTime() - System.currentTimeMillis();
        return diff / 1000;
    }

    private String getKey(int projectId, int jobId) {
        return MessageFormat.format("{0}$$$${1}", projectId, jobId);
    }

    public String[] resolveKey(String key) {
        return key.split("\\$\\$\\$\\$");
    }

}
