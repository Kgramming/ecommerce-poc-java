package com.y3l.supply.web;

/** A 4xx problem with a fixed message — mirrors FastAPI's HTTPException. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() { return status; }

    public static ApiException badRequest(String msg) { return new ApiException(400, msg); }
    public static ApiException notFound(String msg) { return new ApiException(404, msg); }
    public static ApiException unprocessable(String msg) { return new ApiException(422, msg); }
}
