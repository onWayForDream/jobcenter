package cn.enn.portal.jobCenter.core;

import cn.enn.portal.jobCenter.core.util.CronExpressionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.quartz.CronExpression;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

public class TestCronUtil {

//    @Test
    public void testCreateCronExpression() {
        CronExpressionUtil cronExpressionUtil = new CronExpressionUtil();
        String cronExp = cronExpressionUtil.createCronExpression('s', 30, Optional.empty());
        System.out.println("every 30 seconds: cronExp = " + cronExp);
//        Assert.assertTrue(cronExp.equals("/30 * * * * ?"));

        cronExp = cronExpressionUtil.createCronExpression('m', 10, Optional.empty());
        System.out.println("every 10 minutes: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('h', 8, Optional.empty());
        System.out.println("every 8 hours: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('h', 5, Optional.empty());
        System.out.println("every 5 hours: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('d', 1, Optional.empty());
        System.out.println("every 1 day: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('d', 3, Optional.empty());
        System.out.println("every 3 day: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('M', 1, Optional.empty());
        System.out.println("every 1 month: cronExp = " + cronExp);

        cronExp = cronExpressionUtil.createCronExpression('M', 3, Optional.empty());
        System.out.println("every 3 month: cronExp = " + cronExp);


    }

    @Test
    public void getCalender() {
        Date date = new Date();
        System.out.println("now is:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        System.out.println("年:" + calendar.get(Calendar.YEAR));
        System.out.println("月:" + calendar.get(Calendar.MONTH));
        System.out.println("日:" + calendar.get(Calendar.DAY_OF_MONTH));
        System.out.println("时:" + calendar.get(Calendar.HOUR_OF_DAY));
        System.out.println("分:" + calendar.get(Calendar.MINUTE));
        System.out.println("秒:" + calendar.get(Calendar.SECOND));
    }
}
