package kr.co.firstdayproject.dto.corp;

public record CompanyAccountActionResponse(
        boolean success,
        String message,
        String redirectUrl
) {

    public static CompanyAccountActionResponse success(String message) {
        return new CompanyAccountActionResponse(true, message, null);
    }

    public static CompanyAccountActionResponse success(
            String message,
            String redirectUrl
    ) {
        return new CompanyAccountActionResponse(true, message, redirectUrl);
    }

    public static CompanyAccountActionResponse failure(String message) {
        return new CompanyAccountActionResponse(false, message, null);
    }
}
