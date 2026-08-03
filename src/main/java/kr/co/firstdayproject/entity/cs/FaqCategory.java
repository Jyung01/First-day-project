package kr.co.firstdayproject.entity.cs;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 관리 화면 없이 초기 SQL로 고정하는 FAQ 조회·필터 분류
 * DB table: faq_categories
 */
@Entity
@Table(name = "faq_categories")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FaqCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_category_id", nullable = false)
    private Long faqCategoryId;
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
