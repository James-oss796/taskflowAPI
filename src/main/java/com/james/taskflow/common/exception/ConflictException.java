package com.james.taskflow.common.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message){
        super(message);
    }
}
