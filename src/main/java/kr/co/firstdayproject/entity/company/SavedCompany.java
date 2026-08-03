package kr.co.firstdayproject.entity.company;

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
 * 개인회원 관심 기업
 * DB table: saved_companies
 */
@Entity
@Table(name = "saved_companies")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SavedCompany {

    @EmbeddedId
    private SavedCompanyId id;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
