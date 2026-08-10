package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 기존 InquiryAttachmentRepository에 문의별 첨부파일 조회 메서드를 추가한 버전입니다.
 */
@Repository
public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {

    List<InquiryAttachment> findByInquiryId(Long inquiryId);
}