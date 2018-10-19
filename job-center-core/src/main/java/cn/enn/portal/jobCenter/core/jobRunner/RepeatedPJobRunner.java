package cn.enn.portal.jobCenter.core.jobRunner;


import cn.enn.portal.jobCenter.core.JobcenterLoggerProxy;
import cn.enn.portal.jobCenter.core.SupportedRuntime;
import org.json.JSONObject;

import java.io.File;

public class RepeatedPJobRunner extends CommonRepeatedRunner {

    @Override
    public String runUserJob() throws Exception {
        if (jobEntity.getRuntimeType() != null && jobEntity.getRuntimeType().equals(SupportedRuntime.LinuxShell.toString())) {
            String projectHome = jobCenterCoreProperty.getProjectFolder(projectEntity.getId()).getAbsolutePath();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("projectHome", projectHome);
            jsonObject.put("command", jobEntity.getExecuteClass());
            jobParam = jsonObject.toString();
            try {
                // set permission of sh fills, add execute permission
                File file = new File(projectHome);
                File[] shellFiles = file.listFiles((File dir, String name) -> name.endsWith(".sh"));
                if (shellFiles != null && shellFiles.length > 0) {
                    for (File _shell : shellFiles) {
                        if (!_shell.canExecute()) {
                            _shell.setExecutable(true);
                            logger.info("{}add execute permission on file:{}", runIdStr, _shell.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("set file permission error", ex);
            }
        }
        return pJob.executeJob(jobParam, new JobcenterLoggerProxy(logger, runId), configProperties);
    }
}
