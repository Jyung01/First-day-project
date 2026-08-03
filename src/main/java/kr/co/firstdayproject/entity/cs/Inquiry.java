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
 * 1:1 문의와 관리자 답변
 * DB table: inquiries
 */
@Entity
@Table(name = "inquiries")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "inquiry_category_id", nullable = false)
    private Long inquiryCategoryId;
    @Column(name = "title", nullable = false, length = 100)
    private String title;
    @Column(name = "content", nullable = false, length = 1000)
    private String content;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "answer_content", columnDefinition = "LONGTEXT")
    private String answerContent;
    @Column(name = "answered_by")
    private Long answeredBy;
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
