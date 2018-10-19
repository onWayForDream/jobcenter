package cn.enn.portal.jobCenter.core.service;

import cn.enn.portal.jobCenter.core.JobRunStatus;
import cn.enn.portal.jobCenter.core.entity.JobRunLogEntity;
import cn.enn.portal.jobCenter.core.entity.tmpEntity.RunningJobsOfHosts;
import cn.enn.portal.jobCenter.core.repository.JobRunLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class JobRunService {

    Logger logger = LoggerFactory.getLogger(JobRunService.class);

    @Autowired
    JobRunLogRepository jobRunRepository;

    @Value("${logging.path}")
    private String logRoot;

    @Transactional
    public void updateJobRunStatus(JobRunStatus newStatus, String newStatusDescription, int jobRunId) {
        JobRunLogEntity jobRunEntity = jobRunRepository.findById(jobRunId).get();
        jobRunEntity.setJobStatusCode(newStatus.toString());
        jobRunEntity.setJobStatusDescription(newStatusDescription);
        jobRunEntity.setJobEndTime(new Date());
        jobRunRepository.save(jobRunEntity);
    }

    @Transactional
    public JobRunLogEntity save(JobRunLogEntity entity) {
        return jobRunRepository.save(entity);
    }

    /**
     * 获取本次运行的日志
     *
     * @param jobRunLogEntity
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public StringBuffer getRunningDetails(JobRunLogEntity jobRunLogEntity) throws IOException, InterruptedException {

        StringBuffer stringBuffer = findLogByDate(jobRunLogEntity.getJobStartTime(), jobRunLogEntity.getRunId());
        if (jobRunLogEntity.getJobEndTime() == null) {
            return stringBuffer;
        }

        SimpleDateFormat simpleDateFormat_Hour = new SimpleDateFormat("yyyy-MM-dd_HH");
        if (!simpleDateFormat_Hour.format(jobRunLogEntity.getJobStartTime()).equals(simpleDateFormat_Hour.format(jobRunLogEntity.getJobEndTime()))) {
            StringBuffer stringBuffer2 = findLogByDate(jobRunLogEntity.getJobEndTime(), jobRunLogEntity.getRunId());
            stringBuffer.append(stringBuffer2);
        }
        return stringBuffer;

    }

    private StringBuffer findLogByDate(Date date, String uuid) throws IOException {
        SimpleDateFormat simpleDateFormat_Day = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleDateFormat_Hour = new SimpleDateFormat("yyyy-MM-dd_HH");
        String logFile = MessageFormat.format("{0}/{1}/{2}.log", logRoot, simpleDateFormat_Day.format(date), simpleDateFormat_Hour.format(date));
        File file = new File(logFile);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        StringBuffer stringBuffer = new StringBuffer();

        String line = "";
        int notMatchLines = 10;

        Pattern exceptionNameRegex = Pattern.compile("^((java)|(com)|(org)|(cn))\\.");
        Pattern stackTraceRegex = Pattern.compile("^(Caused\\sby:)|(\\s+at\\s)");
        Pattern runIdRegex = Pattern.compile(uuid.replace("-", "\\-"));
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue;
            }
            if (runIdRegex.matcher(line).find()) {
                notMatchLines = 0;
                stringBuffer.append(line.replace("[" + uuid + "]", ""));
                stringBuffer.append('\n');
                continue;
            } else {
                notMatchLines++;
            }

            if (notMatchLines <= 1) {
                if (exceptionNameRegex.matcher(line).find()
                        || stackTraceRegex.matcher(line).find()) {
                    stringBuffer.append(line);
                    stringBuffer.append('\n');
                    notMatchLines = 0;
                }
            }
        }
        return stringBuffer;
    }

    public HashMap<String, Integer> getRunningJobOfHosts() {
        List<RunningJobsOfHosts> list = jobRunRepository.getRunningJobOfHosts();
        HashMap<String, Integer> map = new HashMap<>();
        for (RunningJobsOfHosts item : list) {
            map.put(item.getHost(), item.getJobCount());
        }
        return map;
    }


}
