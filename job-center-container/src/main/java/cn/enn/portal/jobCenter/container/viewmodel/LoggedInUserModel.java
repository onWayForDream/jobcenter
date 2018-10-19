package cn.enn.portal.jobCenter.container.viewmodel;

import cn.enn.portal.jobCenter.core.entity.UserEntity;

public class LoggedInUserModel extends UserEntity {
    private String token;

    public LoggedInUserModel(UserEntity userEntity, String token) {
        this.token = token;
        this.setLastLoginTime(userEntity.getLastLoginTime());
        this.setId(userEntity.getId());
        this.setCreateTime(userEntity.getCreateTime());
        this.setRoleName(userEntity.getRoleName());
        this.setUserName(userEntity.getUserName());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
