package kr.co.firstdayproject.entity.review;

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
 * 기업리뷰·면접후기 도움돼요; 다형 대상은 서비스 계층에서 무결성 검증
 * DB table: review_reactions
 */
@Entity
@Table(name = "review_reactions")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReviewReaction {

    @EmbeddedId
    private ReviewReactionId id;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
