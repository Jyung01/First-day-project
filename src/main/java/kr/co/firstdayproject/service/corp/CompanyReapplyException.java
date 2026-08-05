package kr.co.firstdayproject.service.corp;

import lombok.Getter;

@Getter
public class CompanyReapplyException extends RuntimeException {

    private final String field;

    public CompanyReapplyException(String field, String message) {
        super(message);
        this.field = field;
    }
}
