package cn.enn.portal.jobCenter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class SpringBootClassLoader extends URLClassLoader {
    Logger logger = LoggerFactory.getLogger(SpringBootClassLoader.class);

    private static Map<String, Integer> urlHashCodeMap = new HashMap<>();

    private int urlHashCode;

    public SpringBootClassLoader(URL[] urls, ClassLoader classLoader) {
        super(urls, classLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    // do not call parent to load spring-related classes !!!
                    if (getParent() != null && !isSpringClass(name)) {
                        c = getParent().loadClass(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private boolean isSpringClass(String className) {
        return className.startsWith("org.springframework");
    }


}
