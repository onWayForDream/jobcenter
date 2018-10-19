package cn.enn.portal.jobCenter.core.zk.producer;

import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.zk.consumer.ZkJobLogConsumer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ZkJobLogProducer extends ZkJobProducerBase {

    @Override
    protected String getZnodePath() {
        return zookeeperClient.getJobLogQueuePath() + "/item-";
    }

    private String createCallbackZnode() throws KeeperException, InterruptedException {
        return zooKeeper.create(zookeeperClient.getCallbackQueuePath() + "/cb-", "".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    // znode key = SEQUENTIAL
    // znode value = {"jobRunId":121212,"hostName":"jobcenter-dev2","jobId",12232, "callbackZnode":"/jobcenter/callback/xxxxxx"}
    private void produceJobLog(JobRunLogEntity jobRunLogEntity, String callbackZnode) throws KeeperException, InterruptedException {
        String znode = getZnodePath();
        String hostName = jobRunLogEntity.getRunningHost();
        JSONObject contentJson = new JSONObject();
        contentJson.put("jobRunId", jobRunLogEntity.getId());
        contentJson.put("hostName", hostName);
        contentJson.put("jobId", jobRunLogEntity.getJobId());
        contentJson.put("callbackZnode", callbackZnode);
        String sequentialNode = zooKeeper.create(znode, contentJson.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("produce log job at {},content = {}", sequentialNode, contentJson.toString());
    }

    public StringBuffer getLogsAwait(JobRunLogEntity jobRunLogEntity, int timeoutSeconds) {
        String callbackZnode = null;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            // 生成一个随机的callback node
            callbackZnode = createCallbackZnode();

            // 发送命令给指定的主机
            produceJobLog(jobRunLogEntity, callbackZnode);

            // 接受日志内容
            for (int i = 0; i < timeoutSeconds * 10; i++) {
                if (getCallbackList(callbackZnode, stringBuffer)) {
                    break;
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            logger.error("get log by zookeeper error", e);
        } finally {
            try {
                zooKeeper.delete(callbackZnode, zooKeeper.exists(callbackZnode, false).getVersion());
            } catch (Exception e) {
                logger.error("delete callback znode error:" + callbackZnode, e);
            }
        }
        return stringBuffer;
    }

    private boolean getCallbackList(String callbackZnode, StringBuffer stringBuffer) throws KeeperException, InterruptedException {
        // 接收日志内容
        List<String> childrenList = zooKeeper.getChildren(callbackZnode, false);
        if (childrenList == null || childrenList.size() == 0) {
            logger.debug("empty childrenList");
            return false;
        }
        childrenList = childrenList.stream().sorted().collect(Collectors.toList());
        for (String _znode : childrenList) {
            logger.debug("znode = {}", _znode);
            String fullPath = callbackZnode + "/" + _znode;
            try {
                byte[] data = zooKeeper.getData(fullPath, false, null);
                if (data == null || data.length == 0) {
                    continue;
                }
                String content = new String(data);
                if (content.equals(ZkJobLogConsumer.LOG_END)) {
                    logger.debug("meet the end and return");
                    return true;
                } else {
                    stringBuffer.append(content);
                    logger.debug("append log and continue");
                }
            } catch (Exception ex) {
                logger.error("fetch log error", ex);
            } finally {
                zooKeeper.delete(fullPath, zooKeeper.exists(fullPath, false).getVersion());
            }
        }
        return false;
    }


}
