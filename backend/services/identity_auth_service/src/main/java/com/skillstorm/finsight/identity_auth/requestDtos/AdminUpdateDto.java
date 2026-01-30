package com.skillstorm.finsight.identity_auth.requestDtos;

public class AdminUpdateDto {

    private boolean isActive;
    private Integer roleId;

    public AdminUpdateDto() {
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
