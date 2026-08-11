package kr.co.firstdayproject.entity.site;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 서비스명·로고·푸터·고객센터 등 사이트 설정; 비밀값 저장 금지
 * DB table: site_settings
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SiteSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", nullable = false, columnDefinition = "json")
    private String settingValue;
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}