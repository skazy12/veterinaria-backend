package com.example.demo.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetails error;

    @Data
    public static class ErrorDetails {
        private String code;
        private String message;
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setCode(code);
        errorDetails.setMessage(message);
        response.setError(errorDetails);
        return response;
    }
}
