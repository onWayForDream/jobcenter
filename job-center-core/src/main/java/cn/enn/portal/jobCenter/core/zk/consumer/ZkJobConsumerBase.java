package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.repository.ProjectRepository;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.util.HostNameProvider;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ZkJobConsumerBase implements Watcher {

    @Autowired
    protected ZookeeperClient zookeeperClient;

    @Autowired
    protected JobScheduleService jobScheduleService;

    @Autowired
    protected JobRepository jobRepository;

    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected HostNameProvider hostNameProvider;

    protected ZooKeeper zooKeeper = null;

    protected Logger logger = null;

    public ZkJobConsumerBase() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public void startWatcher() throws IOException, InterruptedException, KeeperException {
        zooKeeper = zookeeperClient.connect();
        String znode = getJobQueuePath();
        zooKeeper.getChildren(znode, this);
        logger.info("begin watch children of znode:{}", znode);
    }

    protected void consumeChildrenList(List<String> childrenList) throws KeeperException, InterruptedException {
        if (childrenList == null || childrenList.size() == 0)
            return;
        childrenList = childrenList.stream().sorted().collect(Collectors.toList());

        for (String znode : childrenList) {
            try {

                String fullPath = getJobQueuePath() + "/" + znode;
                Stat stat = zooKeeper.exists(fullPath, false);
                if (stat == null) {
                    continue;
                }
                byte[] bytes = zooKeeper.getData(fullPath, false, stat);
                if (bytes == null || bytes.length == 0) {
                    logger.info("skip empty znode:{}", fullPath);
                    continue;
                }
                String content = new String(bytes);
                logger.debug("znode content is :{}", content);
                JSONObject contentJson = new JSONObject(content);
                if (!hostNameProvider.getHostName().equals(contentJson.getString("hostName"))) {
//                logger.info("skip job:{}", znode);
                    continue;
                }

                // delete before process
                zooKeeper.delete(fullPath, stat.getVersion());
                // just handle the znode with hostname of this
                processJobItem(znode, contentJson);

            }
            catch (Exception ex) {
                logger.error("consume queue item error:{}", znode, ex);
            }
        }
    }

    protected abstract String getJobQueuePath();

    protected abstract void processJobItem(String znode, JSONObject contentJson) throws Exception;

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType().equals(Event.EventType.NodeChildrenChanged)) {

            try {
                List<String> childrenList = zooKeeper.getChildren(getJobQueuePath(), this);
                consumeChildrenList(childrenList);
            } catch (Exception e) {
                logger.error("getChildren error", e);
            }
        }
    }
}
