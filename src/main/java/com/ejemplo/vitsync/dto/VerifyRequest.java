package com.ejemplo.vitsync.dto;
import lombok.Data;

@lombok.Data
public class VerifyRequest {
    private String email;
    private String code;
}
