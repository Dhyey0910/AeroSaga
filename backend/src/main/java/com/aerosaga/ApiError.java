package com.aerosaga;

import java.time.Instant;

/** JSON body returned for every error. */
public class ApiError {
    public int status;
    public String error;
    public String message;
    public String path;
    public String timestamp;

    public ApiError() {}

    public ApiError(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now().toString();
    }
}
