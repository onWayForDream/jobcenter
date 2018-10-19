package cn.enn.portal.jobCenter.core.zk;

import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LeaderElection implements Watcher {

    @Autowired
    private ZookeeperClient zookeeperClient;

    @Autowired
    private HostNameProvider hostNameProvider;

    @Autowired
    private ShutdownWatcher shutdownWatcher;

    private Logger logger = LoggerFactory.getLogger(LeaderElection.class);

    private String myTicket;
    private String leftHostName;

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            try {
                boolean isLeader = checkLeaderOrContunue();
                if (isLeader) {
                    // 恢复原来的leader
                    shutdownWatcher.restoreRunningJobsOfHost(this.leftHostName);
                }

            } catch (Exception ex) {
                logger.error("LeaderElection process error", ex);
            }
        }
    }


    public boolean elect() throws KeeperException, InterruptedException, IOException {
        ZooKeeper zk = zookeeperClient.connect();
        // register to /${rootNode}/clients
        registerClient(zk);

        String fullPath = zookeeperClient.getElectionPath() + "/queue-";
        String newZnode = zk.create(fullPath, hostNameProvider.getHostName().getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        this.myTicket = newZnode.substring(newZnode.lastIndexOf("queue-"));
        logger.info("my ticket is {}", this.myTicket);
        return checkLeaderOrContunue();
    }

    /**
     * 判断自己是否成为了leader，如果是，则调用leaderInit()方法做一些初始化，如果不是，则继续监听比自己小一号的znode，直到成为leader
     *
     * @return 当前是否是leader
     * @throws KeeperException
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean checkLeaderOrContunue() throws KeeperException, InterruptedException, IOException {
        ZooKeeper zk = zookeeperClient.connect();
        List<String> nodeList = zk.getChildren(zookeeperClient.getElectionPath(), false);
        nodeList = nodeList.stream().sorted().collect(Collectors.toList());
        int index = nodeList.indexOf(myTicket);
        boolean isLeader = index == 0;
        logger.info("{} is leader? {}", hostNameProvider.getHostName(), isLeader);
        if (!isLeader) {
            // 如果选举失败，则监听比自己小一号的znode，如果它消失了，则再次竞选
            String leftNode = nodeList.get(index - 1);
            String fullLeftNode = zookeeperClient.getElectionPath() + "/" + leftNode;
            // 获取比自己小一号的znode所代表的主机名（存放在znode的data中）
            byte[] data = zk.getData(fullLeftNode, null, zk.exists(fullLeftNode, false));
            this.leftHostName = new String(data);
            // 设置watcher，监听比自己小一号的node
            zk.exists(fullLeftNode, this);
            logger.info("set watcher on {}", fullLeftNode);
        } else {
            leaderInit();
        }
        return isLeader;
    }

    private void leaderInit() throws InterruptedException, IOException, KeeperException {
        shutdownWatcher.startWatcher();
        ZooKeeper zk = zookeeperClient.connect();
    }

    private void registerClient(ZooKeeper zk) throws KeeperException, InterruptedException {
        String heartbeatNode = zookeeperClient.getClientPath() + "/" + hostNameProvider.getHostName();
        // content is the count of running jobs on this process, default is 0,
        // which is increased when new job started, see ZkJobStartProducer class.
        zk.create(heartbeatNode, "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        logger.info("{} is online now!", hostNameProvider.getHostName());
    }

}
