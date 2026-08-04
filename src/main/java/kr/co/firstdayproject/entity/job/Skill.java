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
 * 공통 기술 스택 마스터
 * DB table: skills
 */
@Entity
@Table(
    name = "skills",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_skills_parent_name",
            columnNames = {"parent_id", "skill_name"}
        ),
        @UniqueConstraint(
            name = "uk_skills_slug",
            columnNames = "slug"
        )
    },
    indexes = @Index(
        name = "idx_skills_parent_order",
        columnList = "parent_id, is_active, display_order"
    )
)
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
