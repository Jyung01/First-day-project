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
 * 기업 채용담당자 내부 메모
 * DB table: application_memos
 */
@Entity
@Table(name = "application_memos")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApplicationMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_memo_id", nullable = false)
    private Long applicationMemoId;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(name = "memo", nullable = false, columnDefinition = "TEXT")
    private String memo;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
