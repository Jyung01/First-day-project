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
 * 자주 묻는 질문
 * DB table: faqs
 */
@Entity
@Table(name = "faqs")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id", nullable = false)
    private Long faqId;
    @Column(name = "faq_category_id", nullable = false)
    private Long faqCategoryId;
    @Column(name = "question", nullable = false, length = 500)
    private String question;
    @Column(name = "answer", nullable = false, columnDefinition = "LONGTEXT")
    private String answer;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void setUpdatedBy(Long adminId) {
    }
}
