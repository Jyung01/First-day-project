package kr.co.firstdayproject.service.my;

public class MyPageException extends RuntimeException {

    private final String field;

    public MyPageException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
