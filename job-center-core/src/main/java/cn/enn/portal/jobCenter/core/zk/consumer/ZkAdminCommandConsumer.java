package cn.enn.portal.jobCenter.core.zk.consumer;

import cn.enn.portal.jobCenter.core.zk.ZkLock;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ZkAdminCommandConsumer extends ZkJobConsumerBase {

    @Autowired
    private ZkLock uniqueLock;

    @Autowired
    private MaintenanceCommandExecutor maintenanceCommandExecutor;

    @Override
    protected String getJobQueuePath() {
        return super.zookeeperClient.getCommandQueuePath();
    }

    @Override
    protected void processJobItem(String znode, JSONObject contentJson) throws Exception {
        if (contentJson.has("command") && contentJson.getString("command").equals(MaintenanceCommandExecutor.MAINTENANCE_COMMAND_PREFIX)) {
            maintenanceCommandExecutor.shutdown();
        }
    }


}
