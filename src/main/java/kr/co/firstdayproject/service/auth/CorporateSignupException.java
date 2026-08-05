package kr.co.firstdayproject.service.auth;

public class CorporateSignupException extends RuntimeException {

    private final String field;

    public CorporateSignupException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
