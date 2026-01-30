package com.skillstorm.finsight.identity_auth.requestDtos;

public class ChangeEmailDto {
    private String newEmail;

    public ChangeEmailDto() {
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
