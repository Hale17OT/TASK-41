package com.dispatchops.web.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private List<FieldError> errors;
    private String timestamp;

    public ApiResult() {
    }

    private ApiResult(int code, String message, T data, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "OK", data, null);
    }

    public static <Void> ApiResult<Void> success() {
        return new ApiResult<>(200, "OK", null, null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, null);
    }

    public static <T> ApiResult<T> validationError(List<FieldError> errors) {
        return new ApiResult<>(400, "Validation failed", null, errors);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public static class FieldError {

        private String field;
        private String message;

        public FieldError() {
        }

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
