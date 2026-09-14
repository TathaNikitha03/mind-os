package com.mindos.backend.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String name;
    private String mobile;
    private String token;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, Long userId, String name, String mobile, String token) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.name = name;
        this.mobile = mobile;
        this.token = token;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
