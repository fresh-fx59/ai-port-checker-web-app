package com.example.geminiapp.dto;

import java.util.UUID;

public class AuthResponse {
    
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;

    // Default constructor
    public AuthResponse() {}

    // Constructor for success response
    public AuthResponse(boolean success, String message, String token, UserInfo user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Constructor for error response
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static AuthResponse success(String message, String token, UserInfo user) {
        return new AuthResponse(true, message, token, user);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    // Inner class for user information
    public static class UserInfo {
        private UUID id;
        private String email;
        private boolean isAnonymous;
        private long remainingRequests;

        public UserInfo() {}

        public UserInfo(UUID id, String email, boolean isAnonymous, long remainingRequests) {
            this.id = id;
            this.email = email;
            this.isAnonymous = isAnonymous;
            this.remainingRequests = remainingRequests;
        }

        // Getters and Setters
        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isAnonymous() {
            return isAnonymous;
        }

        public void setAnonymous(boolean anonymous) {
            isAnonymous = anonymous;
        }

        public long getRemainingRequests() {
            return remainingRequests;
        }

        public void setRemainingRequests(long remainingRequests) {
            this.remainingRequests = remainingRequests;
        }
    }
}