package kr.co.firstdayproject.entity.job;

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
 * 개인회원 희망 직무; 최대 3개는 서비스 계층에서 트랜잭션 검증
 * DB table: user_desired_jobs
 */
@Entity
@Table(name = "user_desired_jobs")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserDesiredJob {

    @EmbeddedId
    private UserDesiredJobId id;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
