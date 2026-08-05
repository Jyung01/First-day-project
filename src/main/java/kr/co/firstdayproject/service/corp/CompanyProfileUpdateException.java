package kr.co.firstdayproject.service.corp;

import lombok.Getter;

@Getter
public class CompanyProfileUpdateException extends RuntimeException {

    private final String field;

    public CompanyProfileUpdateException(String field, String message) {
        super(message);
        this.field = field;
    }

    public CompanyProfileUpdateException(
            String field,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.field = field;
    }
}
