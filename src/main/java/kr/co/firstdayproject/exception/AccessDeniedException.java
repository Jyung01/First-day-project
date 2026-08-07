package kr.co.firstdayproject.exception;

/** 로그인한 사용자가 소유하지 않은 리소스에 접근할 때 사용. 전역 핸들러에서 403으로 매핑된다. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
