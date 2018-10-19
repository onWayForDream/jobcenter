package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;
import cn.enn.portal.jobCenter.core.service.JobRunService;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Service
public class ZkJobLogConsumer extends ZkJobConsumerBase {

    @Autowired
    private JobRunLogRepository jobRunLogRepository;

    @Autowired
    private JobRunService jobRunService;

    public static final String LOG_END = "$$$LOG_END$$$";

    /**
     * 覆写父类的consumeChildrenList()方法
     * znode key = SEQUENTIAL
     * znode value = {"jobRunId":121212,"hostName":"jobcenter-dev2","jobId",12232, "callbackZnode":"/jobcenter/callback/xxxxxx"}
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    protected void consumeChildrenList(List<String> childrenList) throws KeeperException, InterruptedException {
        if (childrenList == null || childrenList.size() == 0)
            return;
        childrenList = childrenList.stream().sorted().collect(Collectors.toList());
        for (String znode : childrenList) {
            String fullPath = getJobQueuePath() + "/" + znode;

            Stat stat = zooKeeper.exists(fullPath, false);
            if (stat == null) {
                continue;
            }
            byte[] bytes = zooKeeper.getData(fullPath, false, stat);
            if (bytes == null || bytes.length == 0) {
                logger.info("skip empty znode:{}" + fullPath);
                continue;
            }
            String content = new String(bytes);
            JSONObject jsonObject = new JSONObject(content);
            if (!hostNameProvider.getHostName().equals(jsonObject.getString("hostName"))) {
//                logger.info("skip job:{}", znode);
                continue;
            }
            try {
                // just handle the znode with hostname of this
                int jobRunId = jsonObject.getInt("jobRunId");
                // delete before process
                zooKeeper.delete(fullPath, stat.getVersion());
                logger.info("begin to get job log from zk queue, jobId = {}, znode = {}", jsonObject.getInt("jobId"), fullPath);
                this.processJobLog(jobRunId, jsonObject.getString("callbackZnode"));

            } catch (Exception ex) {
                logger.error("consume queue item error:{}", znode, ex);
            }
        }
    }

    @Override
    protected String getJobQueuePath() {
        return super.zookeeperClient.getJobLogQueuePath();
    }

    @Override
    protected void processJobItem(String znode, JSONObject contentJson) throws Exception {
        throw new RuntimeException("not implement");
    }

    /**
     * get job run logs from log file and send content to callback zookeeper znode
     *
     * @param jobRunId
     * @param callbackZnodePath
     */
    private void processJobLog(int jobRunId, String callbackZnodePath) throws IOException, InterruptedException, KeeperException {
        int linesPerZnode = 100;
        JobRunLogEntity jobRunLogEntity = jobRunLogRepository.findById(jobRunId)
                .orElseThrow(() -> new RuntimeException(MessageFormat.format("run_id {0} not found", jobRunId)));

        StringBuffer stringBuffer = jobRunService.getRunningDetails(jobRunLogEntity);
        Scanner scanner = new Scanner(stringBuffer.toString());

        StringBuffer _sb = new StringBuffer();
        int _lines = 0;
        while (scanner.hasNextLine()) {
            _sb.append(scanner.nextLine() + '\n');
            _lines++;
            if (_lines >= linesPerZnode) {
                createZnode(callbackZnodePath, _sb.toString());
                _sb = new StringBuffer();
                _lines = 0;
            }
        }
        if (!_sb.toString().isEmpty()) {
            createZnode(callbackZnodePath, _sb.toString());
        }

        // 发送终止符
        createZnode(callbackZnodePath, LOG_END);

    }

    private void createZnode(String callbackZnodePath, String content) throws KeeperException, InterruptedException {
        zooKeeper.create(callbackZnodePath + "/log-", content.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

}
