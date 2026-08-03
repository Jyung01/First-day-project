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

/**
 * 관리자 수동 사이트 버전·변경내역; 삭제하지 않음
 * DB table: site_versions
 */
@Entity
@Table(name = "site_versions")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SiteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_version_id", nullable = false)
    private Long siteVersionId;
    @Column(name = "version_name", nullable = false, length = 50)
    private String versionName;
    @Column(name = "change_notes", nullable = false, columnDefinition = "LONGTEXT")
    private String changeNotes;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
