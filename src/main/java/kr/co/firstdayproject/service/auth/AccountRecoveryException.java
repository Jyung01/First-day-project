package kr.co.firstdayproject.service.auth;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccountRecoveryException extends RuntimeException {

    private final HttpStatus status;

    public AccountRecoveryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
