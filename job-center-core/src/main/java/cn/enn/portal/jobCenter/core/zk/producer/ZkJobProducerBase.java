package cn.enn.portal.jobCenter.core.zk.producer;

import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


public abstract class ZkJobProducerBase implements InitializingBean {

    protected Logger logger = null;

    @Autowired
    protected ZookeeperClient zookeeperClient;

    @Autowired
    protected HostNameProvider hostNameProvider;

    @Autowired
    protected JobScheduleService jobScheduleService;

    protected ZooKeeper zooKeeper = null;

    protected abstract String getZnodePath();

    @Override
    public void afterPropertiesSet() {
        logger = LoggerFactory.getLogger(getClass());
        zooKeeper = zookeeperClient.connect();
    }

    /**
     * increase the number in '/clients/hostname' node by delta
     *
     * @param delta
     * @param hostName
     * @return return true if update success
     * @throws KeeperException
     * @throws InterruptedException
     */
    private boolean tryUpdateRunningJobCount(int delta, String hostName) throws KeeperException, InterruptedException {
        String znode = zookeeperClient.getClientPath() + "/" + hostName;
        Stat stat = zooKeeper.exists(znode, false);
        if (stat == null) {
            return false;
        }
        byte[] data = zooKeeper.getData(znode, false, stat);
        int runningJobs = Integer.parseInt(new String(data));
        if (runningJobs <= 0 && delta < 0) {
            // can not less than 0
            return true;
        }
        try {
            Stat stat2 = zooKeeper.setData(znode, String.valueOf(runningJobs + delta).getBytes(), stat.getVersion());
            // znode version will increase by 1 when data changed
            return stat.getVersion() + 1 == stat2.getVersion();
        } catch (Exception ex) {
            logger.error("update running count error", ex);
            return false;
        }
    }

    protected boolean updateRunningJobCount(int delta, String hostName) throws KeeperException, InterruptedException {
        boolean success = false;
        int retryTimes = 1000;
        for (int i = 0; i <= retryTimes; i++) {
            if (this.tryUpdateRunningJobCount(delta, hostName)) {
                success = true;
                logger.debug("update running job count by {} success in times:{}", delta, i);
                break;
            }
            logger.debug("update running job count by {} failed in times:{}", delta, i);
        }
        return success;
    }

    protected int getRunningJobsOfHost(String hostName) throws KeeperException, InterruptedException {
        String znode = zookeeperClient.getClientPath() + "/" + hostName;
        Stat stat = zooKeeper.exists(znode, false);
        byte[] data = zooKeeper.getData(znode, false, stat);
        int runningJobs = Integer.parseInt(new String(data));
        return runningJobs;
    }

}
