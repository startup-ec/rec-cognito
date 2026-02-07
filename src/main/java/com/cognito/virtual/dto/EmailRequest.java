package com.cognito.virtual.dto;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class EmailRequest {
    private String recipient;
    private String subject;
    private String content;
}
