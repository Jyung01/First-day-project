package kr.co.firstdayproject.dto.my;

public record RecentApplicationView(
        Long applicationId,
        String companyName,
        String companyInitial,
        String positionTitle,
        String statusLabel,
        String statusVariant
) {
}
