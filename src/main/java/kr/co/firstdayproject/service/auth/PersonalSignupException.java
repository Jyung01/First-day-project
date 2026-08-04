package kr.co.firstdayproject.service.auth;

public class PersonalSignupException extends RuntimeException {

    private final String field;

    public PersonalSignupException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
