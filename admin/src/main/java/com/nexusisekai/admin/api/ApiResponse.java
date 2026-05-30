package com.nexusisekai.admin.api;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiResponse {
    public final int      status;
    public final JsonNode body;
    public final String   error;

    public ApiResponse(int status, JsonNode body, String error) {
        this.status = status;
        this.body   = body;
        this.error  = error;
    }

    public static ApiResponse error(String msg) {
        return new ApiResponse(0, null, msg);
    }

    public boolean isOk()     { return error == null && status >= 200 && status < 300; }
    public boolean hasError() { return error != null || status == 0; }

    public String message() {
        if (error != null) return error;
        if (body != null && body.has("message")) return body.get("message").asText();
        return status + "";
    }
}
