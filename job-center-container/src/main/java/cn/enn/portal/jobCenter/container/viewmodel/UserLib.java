package cn.enn.portal.jobCenter.container.viewmodel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class UserLib {

    public UserLib() {
    }

    public UserLib(String name, Date createTime) {
        this.name = name;
        this.createTime = createTime;
    }

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
