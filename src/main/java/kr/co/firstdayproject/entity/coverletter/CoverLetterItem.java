package kr.co.firstdayproject.entity.coverletter;

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
 * 자기소개서 원본 문항과 현재 답변
 * DB table: cover_letter_items
 */
@Entity
@Table(name = "cover_letter_items")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CoverLetterItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cover_letter_item_id", nullable = false)
    private Long coverLetterItemId;
    @Column(name = "cover_letter_id", nullable = false)
    private Long coverLetterId;
    @Column(name = "question", nullable = false, length = 1000)
    private String question;
    @Column(name = "answer", nullable = false, columnDefinition = "LONGTEXT")
    private String answer;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
