package cn.enn.portal.jobCenter.container;

import cn.enn.portal.jobCenter.core.JobCenterCoreProperty;
import cn.enn.portal.jobCenter.core.service.JobScheduleService;
import cn.enn.portal.jobCenter.core.zk.LeaderElection;
import cn.enn.portal.jobCenter.core.zk.ShutdownWatcher;
import cn.enn.portal.jobCenter.core.zk.consumer.ZkAdminCommandConsumer;
import cn.enn.portal.jobCenter.core.zk.consumer.ZkJobLogConsumer;
import cn.enn.portal.jobCenter.core.zk.consumer.ZkJobStartConsumer;
import cn.enn.portal.jobCenter.core.zk.consumer.ZkJobStopConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private JobScheduleService jobScheduleService;

    @Autowired
    private ZkJobStartConsumer zkJobStartConsumer;

    @Autowired
    private ZkJobStopConsumer zkJobStopConsumer;

    @Autowired
    private ZkJobLogConsumer zkJobLogConsumer;

    @Autowired
    private JobCenterCoreProperty jobCenterCoreProperty;

    @Autowired
    private LeaderElection leaderElection;

    @Autowired
    private ZkAdminCommandConsumer adminCommandConsumer;

    private Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            File jarRoot = new File(jobCenterCoreProperty.getJobJarRoot());
            if (!jarRoot.exists()) {
                jarRoot.mkdir();
            }

            jobScheduleService.getQuartzScheduler().start();
            leaderElection.elect();
            zkJobStartConsumer.startWatcher();
            zkJobStopConsumer.startWatcher();
            zkJobLogConsumer.startWatcher();
            adminCommandConsumer.startWatcher();

        } catch (Exception e) {
            logger.error("onApplicationEvent error", e);
        }
    }

}
