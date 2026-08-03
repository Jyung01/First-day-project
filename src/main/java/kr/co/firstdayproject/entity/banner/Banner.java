package kr.co.firstdayproject.entity.banner;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 메인 등 위치별 운영 배너
 * DB table: banners
 */
@Entity
@Table(name = "banners")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id", nullable = false)
    private Long bannerId;
    @Column(name = "banner_name", nullable = false, length = 200)
    private String bannerName;
    @Column(name = "placement", nullable = false, length = 30)
    private String placement;
    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;
    @Column(name = "link_url", length = 1000)
    private String linkUrl;
    @Column(name = "alt_text", nullable = false, length = 255)
    private String altText;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "starts_at")
    private LocalDateTime startsAt;
    @Column(name = "ends_at")
    private LocalDateTime endsAt;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
