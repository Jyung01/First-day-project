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
 * 관리자 드래그 정렬 대상 계층형 직무 카테고리
 * DB table: job_categories
 */
@Entity
@Table(
    name = "job_categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_job_categories_slug",
            columnNames = "slug"
        ),
        @UniqueConstraint(
            name = "uk_job_categories_parent_name",
            columnNames = {"parent_id", "category_name"}
        )
    },
    indexes = @Index(
        name = "idx_job_categories_parent_order",
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
public class JobCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_category_id", nullable = false)
    private Long jobCategoryId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
