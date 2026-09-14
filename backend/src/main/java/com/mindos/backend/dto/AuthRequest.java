package com.mindos.backend.dto;

public class AuthRequest {
    private String mobile;
    private String password;

    public AuthRequest() {}

    public AuthRequest(String mobile, String password) {
        this.mobile = mobile;
        this.password = password;
    }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
