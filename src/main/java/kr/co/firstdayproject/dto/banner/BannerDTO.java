package kr.co.firstdayproject.dto.banner;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BannerDTO {
    private Long bannerId;

    private String bannerName;

    private String placement;

    private String imageUrl;

    private String linkUrl;

    private String altText;

    private Integer displayOrder;

    private LocalDate startsAt;

    private LocalDate  endsAt;

    private Boolean isActive;

    private String displayStatus;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
