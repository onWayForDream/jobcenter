package cn.enn.portal.jobCenter.core.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Component
public class CronExpressionUtil {
    public String createCronExpression(Character repeatCycle, int repeatValue, Optional<Date> optionalDate) {
        Calendar calendar = Calendar.getInstance();
        char[] cycles = new char[]{'s', 'm', 'h', 'd', 'M', 'w'};
        int[] calendarProp = new int[]{Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.WEEK_OF_MONTH};
        String[] cronValues = new String[6];
        Date from = null;
        if (optionalDate != null && optionalDate.isPresent()) {
            from = optionalDate.get();
        } else {
            calendar.setTime(new Date());
            calendar.add(Calendar.MINUTE, 5);
            from = calendar.getTime();
        }

        calendar.setTime(from);

        boolean fired = false;
        for (int i = 0; i < 6; i++) {
            String curCronSection = "";
            if (!fired) {
                int curDateValue = calendar.get(calendarProp[i]);
                if (cycles[i] == 'M') {
                    curDateValue += 1;
                }
                if (repeatCycle.equals(cycles[i])) {
                    curCronSection = (curDateValue % repeatValue) + "/" + String.valueOf(repeatValue);
                    fired = true;
                } else {
                    curCronSection = String.valueOf(curDateValue);
                }
            } else {
                if (i == 5) {
                    curCronSection = "?";
                } else {
                    curCronSection = "*";
                }
            }
            cronValues[i] = curCronSection;
        }
        return String.join(" ", cronValues);
    }
}
