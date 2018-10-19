package cn.enn.portal.jobCenter.container.controller;

import cn.enn.portal.jobCenter.container.exception.ContainerException;
import cn.enn.portal.jobCenter.container.exception.UnauthorizedException;
import cn.enn.portal.jobCenter.core.service.UserService;
import cn.enn.portal.jobCenter.core.zk.ZookeeperClient;
import cn.enn.portal.jobCenter.core.zk.consumer.MaintenanceCommandExecutor;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ZookeeperClient zookeeperClient;

    /**
     * 转移所有任务、并进入维护模式
     *
     * @param session
     * @param hostname
     * @throws UnauthorizedException
     * @throws KeeperException
     * @throws InterruptedException
     */
    @PostMapping("/maintenance/{hostname}")
    public void maintenanceCommand(HttpSession session, @PathVariable String hostname) throws UnauthorizedException, KeeperException, InterruptedException {
        if (!userService.isAdministrator(session)) {
            throw new UnauthorizedException();
        }
        ZooKeeper zooKeeper = zookeeperClient.connect();
        // 先加入到维护模式列表
        String maintenanceNode = zookeeperClient.getMaintenancePath() + "/" + hostname;
        if (zooKeeper.exists(maintenanceNode, false) == null) {
            zooKeeper.create(maintenanceNode, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        // 再通知那台主机停机
        JSONObject contentJson = new JSONObject();
        contentJson.put("hostName", hostname);
        contentJson.put("command", MaintenanceCommandExecutor.MAINTENANCE_COMMAND_PREFIX);
        String znode = zookeeperClient.getCommandQueuePath() + "/cmd-";
        String sequentialNode = zooKeeper.create(znode, contentJson.toString().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("produce maintenance command at {},content = {}", sequentialNode, contentJson.toString());
    }

    @PostMapping("/exit-maintenance/{hostname}")
    public void exitMaintenanceCommand(HttpSession session, @PathVariable String hostname) throws ContainerException, KeeperException, InterruptedException {
        if (!userService.isAdministrator(session)) {
            throw new UnauthorizedException();
        }
        ZooKeeper zooKeeper = zookeeperClient.connect();
        String maintenanceNode = zookeeperClient.getMaintenancePath() + "/" + hostname;
        Stat stat = zooKeeper.exists(maintenanceNode, false);
        if (stat == null) {
            throw new ContainerException("该节点目前不是维护模式", HttpStatus.BAD_REQUEST);
        }
        zooKeeper.delete(maintenanceNode, stat.getVersion());
    }

    @PostMapping("/rebalance")
    public void rebalanceCommand() {

    }

    @GetMapping("/running-command")
    public List<String> getRunningCommand() {
        return null;
    }


}
