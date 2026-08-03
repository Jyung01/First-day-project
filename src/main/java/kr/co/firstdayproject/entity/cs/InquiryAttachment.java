package kr.co.firstdayproject.entity.cs;

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
 * 1:1 문의 첨부 메타데이터; 파일은 객체 스토리지, 최대 3개는 서비스 검증
 * DB table: inquiry_attachments
 */
@Entity
@Table(name = "inquiry_attachments")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InquiryAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_attachment_id", nullable = false)
    private Long inquiryAttachmentId;
    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
