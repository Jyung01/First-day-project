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
 * 1:1 문의 유형
 * DB table: inquiry_categories
 */
@Entity
@Table(name = "inquiry_categories")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InquiryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_category_id", nullable = false)
    private Long inquiryCategoryId;
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
