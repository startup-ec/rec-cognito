package com.cognito.virtual.dto;


import com.cognito.virtual.entity.UserStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String username;
    private String password;
    private UserStatus status;
}
