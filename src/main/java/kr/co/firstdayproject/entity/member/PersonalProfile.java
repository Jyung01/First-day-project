package kr.co.firstdayproject.entity.member;

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
 * 개인회원 프로필
 * DB table: personal_profiles
 */
@Entity
@Table(name = "personal_profiles")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PersonalProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(name = "address_line1", length = 255)
    private String addressLine1;
    @Column(name = "address_line2", length = 255)
    private String addressLine2;
    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
