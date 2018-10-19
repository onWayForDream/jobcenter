package cn.enn.portal.jobCenter.container.viewmodel;

import cn.enn.portal.jobCenter.ConcurrentOpt;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class JobPostModel {

    private int id;
    private String jobName;
    private String jobParamJson;
    private String scheduleType;
    private String scheduleValue;
    private String executeClass;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validityFrom;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validityTo;
    private ConcurrentOpt concurrentOpt;
    private String propertiesFile;
    private String profileName;
    private String runtimeType;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobParamJson() {
        return jobParamJson;
    }

    public void setJobParamJson(String jobParamJson) {
        this.jobParamJson = jobParamJson;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleValue() {
        return scheduleValue;
    }

    public void setScheduleValue(String scheduleValue) {
        this.scheduleValue = scheduleValue;
    }

    public String getExecuteClass() {
        return executeClass;
    }

    public void setExecuteClass(String executeClass) {
        this.executeClass = executeClass;
    }

    public Date getValidityFrom() {
        return validityFrom;
    }

    public void setValidityFrom(Date validityFrom) {
        this.validityFrom = validityFrom;
    }

    public Date getValidityTo() {
        return validityTo;
    }

    public void setValidityTo(Date validityTo) {
        this.validityTo = validityTo;
    }

    public ConcurrentOpt getConcurrentOpt() {
        return concurrentOpt;
    }

    public void setConcurrentOpt(ConcurrentOpt concurrentOpt) {
        this.concurrentOpt = concurrentOpt;
    }

    public String getPropertiesFile() {
        return propertiesFile;
    }

    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }
}

