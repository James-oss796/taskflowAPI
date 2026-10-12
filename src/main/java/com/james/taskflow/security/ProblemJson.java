package com.james.taskflow.security;


import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

final class ProblemJson {
    private ProblemJson() {
    }

    static void write(HttpServletResponse response, int status, String title, String detail) throws IOException {
        String requestId = MDC.get("requestId");
        String body = "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"%s}".formatted(
                title, status, detail,
                requestId != null ? ",\"requestId\":\"%s\"".formatted(requestId) : "");

        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write(body);
    }
}
