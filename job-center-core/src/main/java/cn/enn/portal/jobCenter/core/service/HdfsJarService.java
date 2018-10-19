package cn.enn.portal.jobCenter.core.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HdfsJarService {

    private FileSystem fs;

    @Value("${hdfs.url}")
    private String uri;

    @Value("${hdfs.project_dir}")
    private String baseFolder;

    @Value("${jobcenter.core.jobJarRoot}")
    private String jarRoot;

    Logger logger = LoggerFactory.getLogger(HdfsJarService.class);

    private HdfsJarService(@Value("${hdfs.url}") String _uri) throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", _uri);
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");

        UserGroupInformation.setConfiguration(conf);
        fs = FileSystem.get(conf);
    }

    public FileStatus[] listProjectJars(int projectId) throws IOException {
        Path path = new Path(getProjectPath(projectId));
        if (!fs.exists(path)) {
            return null;
        }
        return fs.listStatus(path);
    }

    public int getProjectVersion(int projectId) throws IOException {
        FileStatus[] fileStatuses = listProjectJars(projectId);
        if (fileStatuses == null || fileStatuses.length == 0) {
            return -1;
        }
        List<String> files = Arrays.stream(fileStatuses)
                .map(s -> s.getPath().getName())
                .sorted()
                .collect(Collectors.toList());
        return String.join(",", files).hashCode();
    }

    public void putJar(int projectId, InputStream inputStream, String newFileName) throws IOException {
        String fullName = getProjectPath(projectId) + "/" + newFileName;
        OutputStream outputStream = fs.create(new Path(fullName));
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    public boolean deleteJar(int projectId, String fileName) throws IOException {
        String fullName = getProjectPath(projectId) + "/" + fileName;
        Path path = new Path(fullName);
        boolean isDeleted = fs.delete(path, false);
        logger.warn("file was deleted:{}", path.getName());
        // delete local jar
//        File localJarFile = new File(jarRoot + "/" + projectId + "/" + fileName);
//        if (localJarFile.exists()) {
//            localJarFile.delete();
//        }
        return isDeleted;
    }

    public void getJar(int projectId) throws IOException {
        String projectJarRoot = jarRoot + "/" + projectId;
        getJar(projectId, projectJarRoot);
    }

    public void getJar(int projectId, String directory) throws IOException {
        File targetFolder = new File(directory);
        if (!targetFolder.exists()) {
            targetFolder.mkdir();
        }
        FileStatus[] fileStatuses = this.listProjectJars(projectId);
        if (fileStatuses == null || fileStatuses.length == 0) {
            return;
        }
        if (!directory.endsWith("/")) {
            directory += "/";
        }

        // 删除所有本地文件
        deleteLocalFiles(directory);

        // 从hdfs下载所有文件
        for (FileStatus file : fileStatuses) {
            String newFileName = directory + file.getPath().getName();
            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                outputStream = new FileOutputStream(newFileName, false);
                inputStream = fs.open(file.getPath());
                IOUtils.copy(inputStream, outputStream);
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    /**
     * 删除那些在hdfs上已经删除但在本地未被删除的文件
     *
     * @param localDirectory 本地目录
     */
    private void deleteLocalFiles(String localDirectory) {
        List<String> localFiles = Arrays.stream(new File(localDirectory).listFiles()).map(s -> s.getName()).collect(Collectors.toList());
        for (String item : localFiles) {
            File file = new File(localDirectory + item);
            if (file.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    logger.error("delete directory failed:" + file.getAbsolutePath(), e);
                }
            } else {
                file.delete();
            }
        }
    }

    private int findIndex(List<String> list, String item) {
        for (String str : list) {
            if (str.equals(item)) {
                return list.indexOf(str);
            }
        }
        return -1;
    }

    public void createProjectFolder(int projectId) throws IOException {
        Path path = new Path(getProjectPath(projectId));
        if (!fs.exists(path)) {
            fs.mkdirs(path);
        }
        fs.setPermission(path, new FsPermission("777"));
    }

    public String getProjectPath(int projectId) throws IOException {
        return baseFolder + "/" + projectId;
    }

}
