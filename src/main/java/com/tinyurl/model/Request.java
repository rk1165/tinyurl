package com.tinyurl.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Request {

    @NotBlank(message = "longUrl is required")
    private String longUrl;
}
