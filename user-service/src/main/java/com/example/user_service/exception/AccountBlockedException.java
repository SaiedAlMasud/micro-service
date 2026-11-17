package com.example.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException() {
        super("Your account has been blocked");
    }
}

