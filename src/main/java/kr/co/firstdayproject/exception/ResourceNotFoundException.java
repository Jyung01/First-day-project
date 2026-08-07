package kr.co.firstdayproject.exception;

/** 요청한 리소스가 존재하지 않을 때 사용. 전역 핸들러에서 404로 매핑된다. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
