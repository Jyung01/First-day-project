package kr.co.firstdayproject.entity.cs;

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
 * 고객센터 공지사항
 * DB table: notices  (실제 컬럼 기준, category 없음)
 */
@Entity
@Table(name = "notices")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notice {

    public static final String STATUS_DRAFT = "임시저장";
    public static final String STATUS_PUBLISHED = "공개";
    public static final String STATUS_HIDDEN = "비공개";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // DB DEFAULT CURRENT_TIMESTAMP(6) 사용 -> insert 시 값 안 보냄
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // DB DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE 사용 -> insert/update 시 값 안 보냄
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // ===== 도메인 메서드 =====

    public void update(String title, String content, Boolean isPinned, String status) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        boolean wasPublished = STATUS_PUBLISHED.equals(this.status);
        this.status = status;
        if (!wasPublished && STATUS_PUBLISHED.equals(status) && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.isPinned == null) this.isPinned = false;
        if (this.status == null) this.status = STATUS_DRAFT;
        if (STATUS_PUBLISHED.equals(this.status) && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}