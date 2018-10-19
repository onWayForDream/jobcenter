package cn.enn.portal.jobCenter.core.jobRunner;

import cn.enn.portal.jobCenter.JobcenterLogger;
import cn.enn.portal.jobCenter.PJob;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.Properties;

public class LinuxShellJobRunner implements PJob {

    @Override
    public String executeJob(String param, JobcenterLogger logger, Properties properties) throws Exception {
        JSONObject jsonParam = new JSONObject(param);
        if (jsonParam.keySet().size() < 2) {
            throw new InvalidParameterException("cannot run linux shell due to invalid parameters");
        }

        // params is set in RepeatedPJobRunner
        String projectHome = jsonParam.getString("projectHome");
        String command = jsonParam.getString("command");


        Process p = Runtime.getRuntime().exec(command, null, new File(projectHome));
        logger.info(" run linux command [{}] from directory [{}]", command, projectHome);
        p.waitFor();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine()) != null) {
            logger.info("{}", line);
        }
        return "command run success";
    }


}
