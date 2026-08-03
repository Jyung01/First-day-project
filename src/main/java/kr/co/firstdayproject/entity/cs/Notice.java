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
 * DB table: notices
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
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
