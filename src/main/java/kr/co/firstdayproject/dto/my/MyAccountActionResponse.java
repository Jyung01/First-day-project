package kr.co.firstdayproject.dto.my;

public record MyAccountActionResponse(
        boolean success,
        String message,
        String redirectUrl
) {

    public static MyAccountActionResponse success(String message) {
        return new MyAccountActionResponse(true, message, null);
    }

    public static MyAccountActionResponse success(
            String message,
            String redirectUrl
    ) {
        return new MyAccountActionResponse(true, message, redirectUrl);
    }

    public static MyAccountActionResponse failure(String message) {
        return new MyAccountActionResponse(false, message, null);
    }
}
