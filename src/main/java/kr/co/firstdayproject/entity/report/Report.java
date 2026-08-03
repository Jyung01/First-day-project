package kr.co.firstdayproject.entity.report;

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
 * 기업·공고·기업리뷰·면접후기 신고; 다형 대상 무결성은 서비스 계층에서 검증
 * DB table: reports
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long reportId;
    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(name = "reason_code", nullable = false, length = 30)
    private String reasonCode;
    @Column(name = "detail", length = 2000)
    private String detail;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "resolution_action", length = 10)
    private String resolutionAction;
    @Column(name = "resolution_note", length = 2000)
    private String resolutionNote;
    @Column(name = "handled_by")
    private Long handledBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
}
