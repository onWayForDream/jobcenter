package cn.enn.portal.jobCenter.core;

public enum JobRunStatus {
    RUNNING("运行中"),
    DONE("已停止"),
    FAILED("异常"),
    FUTURE_RUN("将要运行");

    JobRunStatus(String status) {
        this.status = status;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
