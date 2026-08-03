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
 * 개인회원 관심 공고
 * DB table: saved_jobs
 */
@Entity
@Table(name = "saved_jobs")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SavedJob {

    @EmbeddedId
    private SavedJobId id;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
