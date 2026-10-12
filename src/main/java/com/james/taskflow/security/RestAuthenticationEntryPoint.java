package com.james.taskflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException{
        response.setHeader("WWW-Authenticate", "Bearer");
        ProblemJson.write(response, 401, "Unauthorized",
                "Authentication is required: send a valid Bearer token");
    }
}
