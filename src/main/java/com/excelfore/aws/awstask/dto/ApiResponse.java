package com.excelfore.aws.awstask.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T result;

    private String error;
    private String errorMessage;

    // Constructor for success response
    public ApiResponse(T data) {
        this.result = data;
    }

    // Constructor for error response
    public ApiResponse(String error, String errorMessage) {
        this.error = error;
        this.errorMessage = errorMessage;
    }
}
