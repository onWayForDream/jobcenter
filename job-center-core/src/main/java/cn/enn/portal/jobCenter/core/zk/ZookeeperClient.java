package cn.enn.portal.jobCenter.core.zk;

import cn.enn.portal.jobCenter.core.repository.JobRepository;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class ZookeeperClient {

    private Logger logger = LoggerFactory.getLogger(ZookeeperClient.class);

    @Value("${zookeeper.quorum}")
    private String zookeeperQuorum;

    @Value("${zookeeper.rootNode}")
    private String rootNode;

    @Autowired
    JobRepository jobRepository;

    private static volatile ZooKeeper singleZookeeper = null;

    public ZooKeeper connect() {
        if (singleZookeeper == null) {
            synchronized (ZookeeperClient.class) {
                if (singleZookeeper == null) {
                    try {
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        logger.info("connecting to zookeeper:" + zookeeperQuorum);
                        singleZookeeper = new ZooKeeper(zookeeperQuorum, 3000, (WatchedEvent event) -> {
                            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                                countDownLatch.countDown();
                            }
                        });
                        countDownLatch.await();
                        // init zookeeper node if not created yet
                        initZnode(singleZookeeper);
                        logger.info("connected to zookeeper!");
                        int clientCount = singleZookeeper.getChildren(getClientPath(), false).size();
                        logger.info("client count = {}", clientCount);
                        if (clientCount == 0) {
                            jobRepository.resetJobStatus();
                            logger.warn("reset all jobs' status to DONE");
                        }
                    } catch (Exception ex) {
                        logger.error("init zookeeper error", ex);
                    }
                }
            }
        }
        return singleZookeeper;
    }

    private void initZnode(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(rootNode, false) == null) {
            zooKeeper.create(rootNode, "a newbi distributed trigger engine".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        String[] znodeArray = {
                getClientPath(),
                getStartJobQueuePath(),
                getStopJobQueuePath(),
                getJobLogQueuePath(),
                getCallbackQueuePath(),
                getElectionPath(),
                getCommandQueuePath(),
                getUniqueLockPath(),
                getMaintenancePath()
        };

        for (String _znode : znodeArray) {
            if (zooKeeper.exists(_znode, false) == null) {
                zooKeeper.create(_znode, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.info("zookeeper node created : {}", _znode);
            }
        }
    }

    public String getClientPath() {
        return rootNode + "/clients";
    }

    public String getStartJobQueuePath() {
        return rootNode + "/jobStartQueue";
    }

    public String getStopJobQueuePath() {
        return rootNode + "/jobStopQueue";
    }

    public String getJobLogQueuePath() {
        return rootNode + "/jobLogQueue";
    }

    public String getCommandQueuePath() {
        return rootNode + "/commandQueue";
    }

    public String getCallbackQueuePath() {
        return rootNode + "/callback";
    }

    public String getElectionPath() {
        return rootNode + "/election";
    }

    public String getUniqueLockPath() {
        return rootNode + "/lock";
    }

    public String getMaintenancePath() {
        return rootNode + "/maintenance";
    }

}
