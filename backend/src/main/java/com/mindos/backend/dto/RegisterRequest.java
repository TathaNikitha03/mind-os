package com.mindos.backend.dto;

public class RegisterRequest {
    private String name;
    private String mobile;
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String name, String mobile, String password) {
        this.name = name;
        this.mobile = mobile;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
