package kr.co.firstdayproject.entity.application;

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
 * 지원완료부터 입사·불합격·채용종료까지 전형 변경 이력
 * DB table: application_status_history
 */
@Entity
@Table(name = "application_status_history")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_status_id", nullable = false)
    private Long applicationStatusId;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "from_status", length = 20)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;
    @Column(name = "change_reason", length = 1000)
    private String changeReason;
    @Column(name = "changed_by")
    private Long changedBy;
    @Column(name = "actor_type", nullable = false, length = 10)
    private String actorType;
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
