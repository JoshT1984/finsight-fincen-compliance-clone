package com.skillstorm.finsight.identity_auth.dtos;

public class AppUserDto {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;

    public AppUserDto() {
    }

    // Getters and setters

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }
}
