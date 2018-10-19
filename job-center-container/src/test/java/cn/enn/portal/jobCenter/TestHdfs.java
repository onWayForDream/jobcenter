package cn.enn.portal.jobCenter;

import cn.enn.portal.jobCenter.core.service.HdfsJarService;
import org.apache.hadoop.fs.FileStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestHdfs {

    @Autowired
    HdfsJarService hdfsJarService;

    @Test
    public void testListFile() throws IOException {
        FileStatus[] fileStatuses = hdfsJarService.listProjectJars(0);
        for (FileStatus item : fileStatuses) {
            System.out.println("found:" + item.getPath().getName());
        }
        Assert.assertTrue(fileStatuses.length > 0);
    }


    @Test
    public void testPut() throws IOException {
        String testFile1 = "logback-spring.xml";
        String testFile2 = "application.yml";
        InputStream inputStream = TestHdfs.class.getClassLoader().getResourceAsStream(testFile1);
        hdfsJarService.putJar(0, inputStream, testFile1);
        inputStream = TestHdfs.class.getClassLoader().getResourceAsStream(testFile2);
        hdfsJarService.putJar(0, inputStream, testFile2);
    }

    @Test
    public void testMkdir() throws IOException {
        hdfsJarService.createProjectFolder(1);
    }

    @Test
    public void testGetFile() throws IOException {
        String directory = "/tmp/testhdfs";
        hdfsJarService.getJar(0, directory);
        Assert.assertTrue(new File(directory).listFiles().length > 0);
    }

}
