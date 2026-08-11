package kr.co.firstdayproject.exception;

public class DuplicateReportException extends RuntimeException {

    public DuplicateReportException(String message) {
        super(message);
    }
}
