package cn.enn.portal.jobCenter.core.entity;

import cn.enn.portal.jobCenter.PJobMetadata;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "job")
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private int projectId;

    @Column
    private String jobName;

    @Column
    private String owner;

    @Column(length = 4000)
    private String jobParamJson;

    @Column(length = 50, nullable = false)
    private String scheduleType;

    @Column(length = 50)
    private String scheduleValue;

    @Column
    private String executeClass;

    @Column
    private int resolverId;

    @Column
    private int isResolverJob;

    @Column
    private Date validityFrom;

    @Column
    private Date validityTo;

    @Column
    private Integer identifierCode;

    @Column
    private Date createTime;

    @Column
    private Date updateTime;

    @Column
    private String runtimeType;

    @Column
    private String status;

    @Column
    private String concurrentOpt;

    @Column
    private String propertiesFile;

    @Column
    private String profileName;

    @Column
    private String runningHost;

    @Column
    private int disabled;

    public void inflateByScheduler(PJobMetadata pJobMetadata) {
        this.jobName = pJobMetadata.getJobName();
        this.jobParamJson = pJobMetadata.getJobParamJson();
        this.scheduleType = pJobMetadata.getScheduleType().toString();
        this.scheduleValue = pJobMetadata.getScheduleValue();
        this.executeClass = pJobMetadata.getExecuteClass();
        this.validityFrom = pJobMetadata.getValidityFrom();
        this.validityTo = pJobMetadata.getValidityTo();
        this.identifierCode = pJobMetadata.getIdentifierCode();
        this.createTime = new Date();
        this.concurrentOpt = pJobMetadata.getConcurrentOpt().toString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public int getResolverId() {
        return resolverId;
    }

    public void setResolverId(int resolverId) {
        this.resolverId = resolverId;
    }

    public int getIsResolverJob() {
        return isResolverJob;
    }

    public void setIsResolverJob(int isResolverJob) {
        this.isResolverJob = isResolverJob;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIdentifierCode() {
        return identifierCode;
    }

    public void setIdentifierCode(Integer identifierCode) {
        this.identifierCode = identifierCode;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
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

    public String getRunningHost() {
        return runningHost;
    }

    public void setRunningHost(String runningHost) {
        this.runningHost = runningHost;
    }

    public int getDisabled() {
        return disabled;
    }

    public void setDisabled(int disabled) {
        this.disabled = disabled;
    }
}
