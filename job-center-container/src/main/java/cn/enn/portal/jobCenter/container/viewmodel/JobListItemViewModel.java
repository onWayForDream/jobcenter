package cn.enn.portal.jobCenter.container.viewmodel;

import cn.enn.portal.jobCenter.container.JobInfoHelper;
import cn.enn.portal.jobCenter.core.JobRunStatus;
import cn.enn.portal.jobCenter.core.entity.JobEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class JobListItemViewModel {
    public JobListItemViewModel() {
    }

    public JobListItemViewModel(JobEntity jobEntity) {
        this(jobEntity, 0);
    }

    public JobListItemViewModel(JobEntity jobEntity, int subJobCount) {
        this.jobId = jobEntity.getId();
        this.jobName = jobEntity.getJobName();
        this.scheduleInfo = JobInfoHelper.getScheduleDiscription(jobEntity);
        this.isJobResolver = jobEntity.getIsResolverJob() == 1;
        this.updateTime = jobEntity.getUpdateTime();
        this.subJobCount = subJobCount;
        this.scheduleType = jobEntity.getScheduleType();
        this.scheduleValue = jobEntity.getScheduleValue();
        this.jobParamJson = jobEntity.getJobParamJson();
        this.validityFrom = jobEntity.getValidityFrom();
        this.validityTo = jobEntity.getValidityTo();
        this.executeClass = jobEntity.getExecuteClass();
        this.concurrentOpt = jobEntity.getConcurrentOpt();
        this.propertiesFile = jobEntity.getPropertiesFile();
        this.profileName = jobEntity.getProfileName();
        this.runtimeType = jobEntity.getRuntimeType();
        this.disabled = jobEntity.getDisabled();
        if (this.disabled == 1) {
            this.jobName += "(已禁用)";
        }
//        this.status = jobEntity.getStatus();
        if (jobEntity.getStatus() != null && !jobEntity.getStatus().isEmpty()) {
            JobRunStatus jobRunStatus = JobRunStatus.valueOf(jobEntity.getStatus());
            this.status = jobRunStatus.getStatus();
        }
    }


    private int jobId;
    private String jobName;
    private String status;
    private String scheduleInfo;
    private boolean isJobResolver;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    private int subJobCount;
    private String scheduleType;
    private String scheduleValue;
    private String jobParamJson;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validityFrom;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validityTo;
    private String executeClass;
    private String concurrentOpt;
    private String propertiesFile;
    private String profileName;
    private String runtimeType;
    private int disabled;

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScheduleInfo() {
        return scheduleInfo;
    }

    public void setScheduleInfo(String scheduleInfo) {
        this.scheduleInfo = scheduleInfo;
    }

    public boolean isJobResolver() {
        return isJobResolver;
    }

    public void setJobResolver(boolean jobResolver) {
        isJobResolver = jobResolver;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getSubJobCount() {
        return subJobCount;
    }

    public void setSubJobCount(int subJobCount) {
        this.subJobCount = subJobCount;
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

    public String getJobParamJson() {
        return jobParamJson;
    }

    public void setJobParamJson(String jobParamJson) {
        this.jobParamJson = jobParamJson;
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

    public String getExecuteClass() {
        return executeClass;
    }

    public void setExecuteClass(String executeClass) {
        this.executeClass = executeClass;
    }

    public String getConcurrentOpt() {
        return concurrentOpt;
    }

    public void setConcurrentOpt(String concurrentOpt) {
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

    public int getDisabled() {
        return disabled;
    }

    public void setDisabled(int disabled) {
        this.disabled = disabled;
    }
}
