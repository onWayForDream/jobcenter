package cn.enn.portal.jobCenter.core.jobLauncher;

import cn.enn.portal.jobCenter.core.service.HdfsJarService;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderVersion {

    public ClassLoaderVersion(URLClassLoader classLoader, int version) {
        this.classLoader = classLoader;
        this.version = version;
    }

    private URLClassLoader classLoader;
    private int version;

    public URLClassLoader getClassLoader() {
        return classLoader;
    }


    public static boolean isLatestVersion(ClassLoaderVersion classLoaderVersion, int latestVersion) {
        return latestVersion == classLoaderVersion.version;
    }

}
