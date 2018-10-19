package cn.enn.portal.jobCenter.core;

public enum UserRole {

    // 目前的ROLE_USER具备ROLE_DEVELOPER的权限，ROLE_DEVELOPER这个角色目前还不存在

    /**
     * 普通用户，仅具有project下的查看权限，无法上传/下载lib，无法创建/修改/删除/启动/停止job
     */
    ROLE_USER,

    /**
     * 开发者，具有project相关的所有权限
     */
    ROLE_DEVELOPER,

    /**
     * 超级管理员，具有最高权限
     */
    ROLE_ADMIN
}
