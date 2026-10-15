package com.example.schedule.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String username;
    private String rol;
    private Long profesorId;
    @Builder.Default
    private String tokenType = "Bearer";
}
