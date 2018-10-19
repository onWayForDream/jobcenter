package cn.enn.portal.jobCenter.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "job_schedule_log")
public class JobScheduleLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String uuid;

    @Column
    private int jobId;

    @Column
    private String jobName;

    @Column
    private String actionType;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Column
    private String operator;

    @Column(length = 4000)
    private String jobParamJson;

    @Column(length = 50, nullable = false)
    private String scheduleType;

    @Column(length = 50)
    private String scheduleValue;

    @Column
    private String executeClass;

    @Column
    private int projectId;

    @Column
    private String runningHost;

    public void inflateBySchedulerEntity(JobEntity jobEntity){
        this.projectId = jobEntity.getProjectId();
        this.executeClass = jobEntity.getExecuteClass();
        this.jobParamJson = jobEntity.getJobParamJson();
        this.jobName = jobEntity.getJobName();
        this.scheduleType = jobEntity.getScheduleType();
        this.scheduleValue = jobEntity.getScheduleValue();
        this.jobId = jobEntity.getId();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getRunningHost() {
        return runningHost;
    }

    public void setRunningHost(String runningHost) {
        this.runningHost = runningHost;
    }
}
