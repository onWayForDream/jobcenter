package cn.enn.portal.jobCenter.core.zk;

import cn.enn.portal.jobCenter.core.service.JobRunService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * zookeeper中的/clients下的各个节点保存着各自运行job的总数，启动新job时，这个数量也决定这将下一个job运行到哪一台主机上。
 * 但这个数量有可能是不准的，而数据库中是最准确的。所以，该类就是专门用来同步每个节点运行job的总个数，他会读取数据库中各个节点最新的运行总数，然后更新到zookeeper中。
 */
@Component
public class RunningJobCountSync {

    @Autowired
    private ZookeeperClient zookeeperClient;

    @Autowired
    private JobRunService jobRunService;

    Logger logger = LoggerFactory.getLogger(RunningJobCountSync.class);

    public void syncJobCount() throws KeeperException, InterruptedException {
        HashMap<String, Integer> map = jobRunService.getRunningJobOfHosts();
        ZooKeeper zk = zookeeperClient.connect();
        List<String> clients = zk.getChildren(zookeeperClient.getClientPath(), false);
        for (String cli : clients) {
            if (!map.containsKey(cli)) {
                continue;
            }
            String znode = zookeeperClient.getClientPath() + "/" + cli;
            Stat stat = zk.exists(znode, false);
            if (stat != null) {
                byte[] data = zk.getData(znode, false, stat);
                int zkCount = Integer.parseInt(new String(data));
                int dbCount = map.get(cli);
                if (zkCount != dbCount) {
                    try {
                        zk.setData(znode, String.valueOf(dbCount).getBytes(), stat.getVersion());
                        logger.info("modify {}' job count from {} to {}", cli, zkCount, dbCount);
                    } catch (Exception ex) {
                        // 如果版本不对，则无需再试了
                        ;
                    }
                }
            }
        }
    }

}
