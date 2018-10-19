package cn.enn.portal.jobCenter.core.jobRunner;

import cn.enn.portal.jobCenter.core.entity.JobEntity;
import com.google.common.util.concurrent.Striped;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public class PJobRunLock {

    private static Striped<Lock> locks = Striped.lazyWeakLock(100);

    public static Lock getLockForJob(int jobId) {
        String key = String.valueOf(jobId);
        return locks.get(key);
    }

    private static Map<Integer, Integer> activeJobAndInstancesMap = new ConcurrentHashMap<>();

    public static synchronized void notifyActive(JobEntity job) {
        Integer instanceCount = activeJobAndInstancesMap.get(job.getId());
        if (instanceCount == null) {
            instanceCount = 0;
        }
        activeJobAndInstancesMap.put(job.getId(), instanceCount + 1);
    }

    public static synchronized void notifySuspend(JobEntity job) {
        Integer instanceCount = activeJobAndInstancesMap.get(job.getId());
        if (instanceCount != null && instanceCount > 0) {
            int _count = instanceCount - 1;
            if (_count == 0) {
                activeJobAndInstancesMap.remove(job.getId());
            } else {
                activeJobAndInstancesMap.put(job.getId(), _count);
            }
        }
    }

    public static boolean isJobActive(int jobId) {
        Integer instanceCount = activeJobAndInstancesMap.get(jobId);
        return instanceCount != null && instanceCount > 0;
    }

    public static int getActiveJobCount() {
        return activeJobAndInstancesMap.size();
    }

}
