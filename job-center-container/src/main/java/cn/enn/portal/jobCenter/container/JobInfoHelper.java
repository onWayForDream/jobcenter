package cn.enn.portal.jobCenter.container;

import cn.enn.portal.jobCenter.ScheduleType;
import cn.enn.portal.jobCenter.core.entity.JobEntity;

import java.util.Arrays;
import java.util.List;

public class JobInfoHelper {

    public static String getScheduleDiscription(JobEntity jobEntity) {
        ScheduleType scheduleType = ScheduleType.valueOf(jobEntity.getScheduleType());
        switch (scheduleType) {
            case MANUAL:
                return "只运行一次";
            case INTERVAL:
                int seconds = Integer.parseInt(jobEntity.getScheduleValue()) / 1000;
                String timeString = getUpperTimeString(seconds);
                return "每隔" + timeString + "运行";
            case CRON_EXPRESSION:
                return jobEntity.getScheduleValue();
            default:
                return "";
        }
    }

    public static String getUpperTimeString(int seconds) {
        List<Character> unitArray = Arrays.asList('s', 'm', 'h');
        Character unitSection = 's';
        int number = seconds;
        int unitIndex = unitArray.indexOf(unitSection);
        while (number >= 60 && number % 60 == 0 && unitIndex < 2) {
            number = number / 60;
            unitIndex++;
        }
        if (unitIndex == 2 && number % 24 == 0) {
            number = number / 24;
            return number + "天";
        } else {
            return number + unitArray.get(unitIndex).toString();
        }
    }

}
