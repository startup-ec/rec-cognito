package com.cognito.virtual.dto;

import com.cognito.virtual.entity.UserStatus;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class UserInfo {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private java.util.Set<String> roles;
    private java.util.Set<String> permissions;

    private Long id;
    private UserStatus status;
    private Boolean enabled;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private java.time.LocalDateTime lastLogin;
}