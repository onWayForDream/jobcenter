package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.SpringApplicationContextProvider;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

@Component
public class SpringBootJobLauncher extends JobLauncherBase {

    @Autowired
    private SpringContextGhost springContextGhost;

    private Logger logger = LoggerFactory.getLogger(SpringBootJobLauncher.class);

    @Override
    public PJob loadPJob(ProjectEntity project, JobEntity job) throws Exception {
        ClassLoader classLoader = getProjectClassLoader(project);
        Object springContext = getSpringApplicationContext(classLoader);
        return (PJob) springContextGhost.getJobBean(job.getExecuteClass(), springContext);
    }

    @Override
    public PJobResolver loadPJobResolver(ProjectEntity project, JobEntity job) throws Exception {
        ClassLoader classLoader = getProjectClassLoader(project);
        Object springContext = getSpringApplicationContext(classLoader);
        return (PJobResolver) springContextGhost.getJobBean(job.getExecuteClass(), springContext);
    }

    @Override
    protected URL[] resolveJars(int projectId) throws IOException, InterruptedException {
        URL[] urls = super.resolveJars(projectId);
        // unzip the tar file packaged by spring-boot-maven-plugin
        File projectFolder = jobCenterCoreProperty.getProjectFolder(projectId);
        unzipJar(projectFolder, new File(urls[0].getFile()).getName());
        return appendCLassesForSpringBoot(urls, projectId);
    }

    @Override
    protected void OnClassLoaderReady(ClassLoader classLoader) throws Exception {
        // run main function in org.springframework.boot.loader.JarLauncher class, create the spring context
        Class jarLauncherClass = classLoader.loadClass("org.springframework.boot.loader.JarLauncher");
        Method method = jarLauncherClass.getMethod("main", String[].class);
        String[] params = new String[]{};
        method.invoke(null, (Object) params);
    }

    @Override
    protected void onClassLoaderUnload(ClassLoader classLoader) {
        try {
            Object springContext = getSpringApplicationContext(classLoader);
            springContextGhost.close(springContext);
            logger.info("spring application context closed");
        } catch (Exception e) {
            logger.error("spring boot project unload classloader error", e);
        }
        super.onClassLoaderUnload(classLoader);
    }

    private Object getSpringApplicationContext(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class providerClass = classLoader.loadClass(SpringApplicationContextProvider.class.getName());
        Method method2 = providerClass.getDeclaredMethod("getSpringApplicationContext");
        return method2.invoke(null);
    }

    private URL[] appendCLassesForSpringBoot(URL[] urls, int projectId) throws MalformedURLException {
        File projectFolder = jobCenterCoreProperty.getProjectFolder(projectId);
        String classFolder = projectFolder.getAbsolutePath() + "/BOOT-INF/classes";
        String curFolder = projectFolder.getAbsolutePath();
        File libsFolder = new File(projectFolder.getAbsolutePath() + "/BOOT-INF/lib");
        File[] libs = libsFolder.listFiles(filename -> filename.isFile() && filename.getAbsolutePath().endsWith(".jar"));
        URL[] newArray = new URL[2 + libs.length];
        int n = 0;
        for (int i = 0; i < libs.length; i++) {
            newArray[i] = libs[i].toURL();
            n++;
        }
        newArray[n] = new File(classFolder).toURL();
        newArray[n + 1] = new File(curFolder).toURL();
        return newArray;
    }

    private void unzipJar(File destinationDir, String jarName) throws IOException, InterruptedException {
        String command = "unzip -o " + jarName;
        logger.info("run command :{}", command);
        Process p = Runtime.getRuntime().exec(command, null, destinationDir);
        p.waitFor();
    }

}
