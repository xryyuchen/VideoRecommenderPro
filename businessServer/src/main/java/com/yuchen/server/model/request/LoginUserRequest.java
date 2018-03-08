package com.yuchen.server.model.request;

/**
 * @author yuchen
 */
public class LoginUserRequest {
    private String username;

    private String password;

    public String getUsername() {
        return username;
    }

    public LoginUserRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
