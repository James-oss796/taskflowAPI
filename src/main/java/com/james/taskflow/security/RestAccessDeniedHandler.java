package com.james.taskflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;


@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException{
        ProblemJson.write(response, 403, "Forbidden", "You do not have permission to perform this action");
    }
}
