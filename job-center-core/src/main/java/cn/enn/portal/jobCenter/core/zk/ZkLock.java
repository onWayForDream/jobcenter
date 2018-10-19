package cn.enn.portal.jobCenter.core.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class ZkLock {

    private Logger logger = LoggerFactory.getLogger(ZkLock.class);

    @Autowired
    private ZookeeperClient zookeeperClient;

    public boolean lock(String commandName, String commandDescription) {
        ZooKeeper zk = zookeeperClient.connect();
        String znode = zookeeperClient.getUniqueLockPath() + "/" + commandName;

        try {
            if (zk.exists(znode, false) != null) {
                return false;
            }

            zk.create(znode, commandDescription.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return true;
        } catch (Exception e) {
            logger.error("lock " + commandName + " error", e);
            return false;
        }
    }

    public void unlock(String commandName) {
        ZooKeeper zk = zookeeperClient.connect();
        try {
            String znode = zookeeperClient.getUniqueLockPath() + "/" + commandName;
            zk.delete(znode, zk.exists(znode, false).getVersion());
        } catch (Exception e) {
            ;
        }
    }

    public List<String> getLocks() {
        ZooKeeper zk = zookeeperClient.connect();
        try {
            return zk.getChildren(zookeeperClient.getUniqueLockPath(), false);
        } catch (Exception e) {
            logger.error("get all locks error", e);
            return new LinkedList<>();
        }
    }
}
