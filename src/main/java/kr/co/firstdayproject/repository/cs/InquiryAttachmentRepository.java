package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {
}
