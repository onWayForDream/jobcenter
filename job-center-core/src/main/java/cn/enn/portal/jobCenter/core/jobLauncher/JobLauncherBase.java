package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.PJob;
import cn.enn.portal.jobCenter.PJobResolver;
import cn.enn.portal.jobCenter.core.JobCenterCoreProperty;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import cn.enn.portal.jobCenter.core.entity.ProjectEntity;
import cn.enn.portal.jobCenter.core.exception.IncompatibleClassException;
import cn.enn.portal.jobCenter.core.repository.JobRepository;
import cn.enn.portal.jobCenter.core.service.HdfsJarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;

public class JobLauncherBase implements JobLauncher {

    @Autowired
    private HdfsJarService hdfsJarService;

    @Autowired
    JobCenterCoreProperty jobCenterCoreProperty;

    @Autowired
    JobRepository jobRepository;

    private Logger logger = LoggerFactory.getLogger(JobLauncherBase.class);

    private static Map<Integer, ClassLoaderVersion> classLoaderMap = new HashMap<>();

    @Override
    public PJob loadPJob(ProjectEntity project, JobEntity job) throws Exception {
        ClassLoader classLoader = getProjectClassLoader(project);
        Class jobCls = classLoader.loadClass(job.getExecuteClass());
        checkAssignable(PJob.class, jobCls);
        return (PJob) jobCls.newInstance();
    }

    @Override
    public PJobResolver loadPJobResolver(ProjectEntity project, JobEntity job) throws Exception {
        ClassLoader classLoader = getProjectClassLoader(project);
        Class resolverCls = classLoader.loadClass(job.getExecuteClass());
        checkAssignable(PJobResolver.class, resolverCls);
        return (PJobResolver) resolverCls.newInstance();
    }

    protected final ClassLoader getProjectClassLoader(ProjectEntity project) throws Exception {
        // check classloader is exist, create if not
        if (!classLoaderMap.containsKey(project.getId())) {
            synchronized (JobLauncherBase.class) {
                if (!classLoaderMap.containsKey(project.getId())) {
                    createClassLoader(project.getId());
                    logger.info("load classloader of project:{}", project.getId());
                }
            }
        }

        // check classloader is latest, create new one if not
        ClassLoaderVersion classLoaderVersion = classLoaderMap.get(project.getId());
        int latestVersion = hdfsJarService.getProjectVersion(project.getId());
        if (!ClassLoaderVersion.isLatestVersion(classLoaderVersion, latestVersion)) {
            synchronized (JobLauncherBase.class) {
                classLoaderVersion = classLoaderMap.get(project.getId());
                latestVersion = hdfsJarService.getProjectVersion(project.getId());
                if (!ClassLoaderVersion.isLatestVersion(classLoaderVersion, latestVersion)) {
                    onClassLoaderUnload(classLoaderVersion.getClassLoader());
                    createClassLoader(project.getId());
                    logger.info("reload classloader of project:{}", project.getId());
                }
            }
        }

        // return the classloader with latest jars

        return classLoaderMap.get(project.getId()).getClassLoader();
    }

    private void createClassLoader(int projectId) throws Exception {
        URL[] urls = this.resolveJars(projectId);
        URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
        OnClassLoaderReady(classLoader);
        classLoaderMap.put(projectId, new ClassLoaderVersion(classLoader, hdfsJarService.getProjectVersion(projectId)));
    }

    @Override
    public Properties loadConfigProperties(ProjectEntity project, JobEntity job) throws Exception {
        if (job == null || job.getPropertiesFile() == null || job.getPropertiesFile().isEmpty()) {
            return null;
        }
        Properties properties = new Properties();

        ClassLoader classLoader = getProjectClassLoader(project);
        InputStream inputStream = classLoader.getResourceAsStream(job.getPropertiesFile());
        if (inputStream != null) {
            properties.load(inputStream);
        }
        if (job.getProfileName() != null && !job.getProfileName().isEmpty()) {
            int lastIndex = job.getPropertiesFile().lastIndexOf(".properties");
            if (lastIndex < 0) {
                return properties;
            }
            String profileBasedName = job.getPropertiesFile().substring(0, lastIndex) + "-" + job.getProfileName() + ".properties";
            inputStream = classLoader.getResourceAsStream(profileBasedName);
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

    /**
     * expose a hook function to child class to do something after the classloader is initiated
     *
     * @param classLoader the new classloader instance
     * @throws Exception
     */
    protected void OnClassLoaderReady(ClassLoader classLoader) throws Exception {
    }

    /**
     * expose a hook function to child class to do something before the classloader is unload
     *
     * @param classLoader
     */
    protected void onClassLoaderUnload(ClassLoader classLoader) {
    }

    /**
     * check is the new loaded targetClass implements the interfaceClass
     *
     * @param interfaceClass PJob or PJobResolver
     * @param targetClass    user class implements PJob or PJobResolver
     */
    private void checkAssignable(Class interfaceClass, Class targetClass) {
        if (!interfaceClass.isAssignableFrom(targetClass)) {
            throw new IncompatibleClassException(MessageFormat.format("{0} must implement the {1} interface",
                    targetClass.toString(), interfaceClass.toString()));
        }
    }


    protected URL[] resolveJars(int projectId)
            throws IOException, InterruptedException {
        // fetch libs of target project from hdfs
        hdfsJarService.getJar(projectId);

        List<URL> urlList = new LinkedList<>();

        // list libs of target project
        File projectFolder = jobCenterCoreProperty.getProjectFolder(projectId);
        fillWithJar(projectFolder, urlList);

        URL[] urlArray = new URL[urlList.size()];
        for (int i = 0; i < urlList.size(); i++) {
            urlArray[i] = urlList.get(i);
        }
        return urlArray;
    }

    private void fillWithJar(File directory, List<URL> urlList) throws IOException {
        if (!directory.isDirectory()) {
            throw new FileNotFoundException("can not find jar directory:" + directory.getAbsolutePath());
        }
        File[] jars = directory.listFiles(filename -> filename.isFile() && filename.getAbsolutePath().endsWith(".jar"));
        for (int i = 0; i < jars.length; i++) {
            urlList.add(jars[i].toURL());
        }
    }
}
