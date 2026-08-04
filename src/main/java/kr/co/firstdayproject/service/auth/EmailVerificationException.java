package kr.co.firstdayproject.service.auth;

import org.springframework.http.HttpStatus;

public class EmailVerificationException extends RuntimeException {

    private final HttpStatus status;

    public EmailVerificationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
