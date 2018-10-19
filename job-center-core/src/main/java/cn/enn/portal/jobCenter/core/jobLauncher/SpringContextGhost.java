package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Component
public class SpringContextGhost {

    Logger logger = LoggerFactory.getLogger(SpringContextGhost.class);


    public Object getJobBean(String jobName, Object context) throws Exception {
        // call getBean() method by reflection
        // ConfigurableApplicationContext configurableApplicationContext = null;
        // configurableApplicationContext.getBean("name");

        Method method = context.getClass().getMethod("getBean", String.class);
        return method.invoke(context, jobName);
    }

    public void close(Object context) throws Exception {
        // call close method by reflection
//         ConfigurableApplicationContext configurableApplicationContext = null;
//         configurableApplicationContext.close();
        Method method = context.getClass().getMethod("close");
        method.invoke(context);
    }

}
