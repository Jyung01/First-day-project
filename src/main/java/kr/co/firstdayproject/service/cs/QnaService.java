package kr.co.firstdayproject.service.cs;

import jakarta.persistence.criteria.Predicate;
import kr.co.firstdayproject.dto.admin.InquiryDetailDto;
import kr.co.firstdayproject.dto.admin.InquiryListItemDto;
import kr.co.firstdayproject.entity.cs.Inquiry;
import kr.co.firstdayproject.entity.cs.InquiryAttachment;
import kr.co.firstdayproject.entity.cs.InquiryCategory;
import kr.co.firstdayproject.repository.cs.InquiryAttachmentRepository;
import kr.co.firstdayproject.repository.cs.InquiryCategoryRepository;
import kr.co.firstdayproject.repository.cs.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static final String STATUS_RECEIVED = "접수";     // 접수(미답변, 기본값)
    public static final String STATUS_IN_PROGRESS = "처리중"; // 처리중(미답변)
    public static final String STATUS_ANSWERED = "답변완료";   // 답변완료

    /** 프론트 필터 파라미터("PENDING"/"ANSWERED")를 실제 DB 상태값 목록으로 변환 */
    private static final List<String> PENDING_STATUSES = List.of(STATUS_RECEIVED, STATUS_IN_PROGRESS);

    private final InquiryRepository inquiryRepository;
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;

    /** 노출용 활성 카테고리 목록 (등록순) */
    public List<InquiryCategory> getActiveCategories() {
        return inquiryCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /** 관리자 대시보드 - 미답변 문의 건수 */
    public long getPendingCount() {
        return inquiryRepository.count(statusIn(PENDING_STATUSES));
    }

    /** 관리자 - 1:1 문의 목록 검색/페이징 */
    public Page<InquiryListItemDto> getInquiryList(Long categoryId, String status, String keyword, Pageable pageable) {
        Map<Long, String> categoryNameMap = getActiveCategories().stream()
                .collect(Collectors.toMap(InquiryCategory::getInquiryCategoryId, InquiryCategory::getCategoryName));

        Specification<Inquiry> spec = Specification.where(categoryEquals(categoryId))
                .and(statusFilter(status))
                .and(keywordContains(keyword));

        Page<Inquiry> inquiries = inquiryRepository.findAll(spec, pageable);

        return inquiries.map(inquiry -> toListItem(inquiry, categoryNameMap));
    }

    /** 관리자 - 문의 상세 (답변 모달 데이터) */
    public InquiryDetailDto getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        String categoryName = inquiryCategoryRepository.findById(inquiry.getInquiryCategoryId())
                .map(InquiryCategory::getCategoryName)
                .orElse("-");

        List<InquiryAttachment> attachments = inquiryAttachmentRepository.findByInquiryId(inquiryId);

        return InquiryDetailDto.builder()
                .inquiryId(inquiry.getInquiryId())
                .categoryName(categoryName)
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .writerLabel("회원 #" + inquiry.getUserId()) // TODO: User 엔티티 연동 시 실제 이름/유형으로 교체
                .createdAt(inquiry.getCreatedAt().format(DATETIME_FORMAT))
                .status(inquiry.getStatus())
                .statusLabel(toStatusLabel(inquiry.getStatus()))
                .answerContent(inquiry.getAnswerContent())
                .answeredAt(inquiry.getAnsweredAt() == null ? null : inquiry.getAnsweredAt().format(DATETIME_FORMAT))
                .attachments(attachments.stream()
                        .map(a -> InquiryDetailDto.Attachment.builder()
                                .attachmentId(a.getInquiryAttachmentId())
                                .originalName(a.getOriginalName())
                                .fileSize(a.getFileSize())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    /** 관리자 - 답변 등록 */
    @Transactional
    public void answerInquiry(Long inquiryId, String answerContent, Long adminId) {
        validateAnswerContent(answerContent);

        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        inquiry.setAnswerContent(answerContent.trim());
        inquiry.setAnsweredBy(adminId);
        inquiry.setAnsweredAt(LocalDateTime.now());
        inquiry.setStatus(STATUS_ANSWERED);
        inquiry.setUpdatedAt(LocalDateTime.now());
    }

    /** 관리자 - 답변 수정 */
    @Transactional
    public void updateAnswer(Long inquiryId, String answerContent) {
        validateAnswerContent(answerContent);

        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        if (!STATUS_ANSWERED.equals(inquiry.getStatus())) {
            throw new IllegalStateException("아직 답변이 등록되지 않은 문의입니다.");
        }

        inquiry.setAnswerContent(answerContent.trim());
        inquiry.setUpdatedAt(LocalDateTime.now());
    }

    /** 관리자 - 답변 삭제 (미답변 상태로 되돌림) */
    @Transactional
    public void deleteAnswer(Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        inquiry.setAnswerContent(null);
        inquiry.setAnsweredBy(null);
        inquiry.setAnsweredAt(null);
        inquiry.setStatus(STATUS_RECEIVED);
        inquiry.setUpdatedAt(LocalDateTime.now());
    }

    private Inquiry getInquiryOrThrow(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의를 찾을 수 없습니다. id=" + inquiryId));
    }

    private void validateAnswerContent(String answerContent) {
        if (!StringUtils.hasText(answerContent)) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }
        if (answerContent.trim().length() > 2000) {
            throw new IllegalArgumentException("답변은 2000자 이내로 입력해주세요.");
        }
    }

    private InquiryListItemDto toListItem(Inquiry inquiry, Map<Long, String> categoryNameMap) {
        return InquiryListItemDto.builder()
                .inquiryId(inquiry.getInquiryId())
                .categoryName(categoryNameMap.getOrDefault(inquiry.getInquiryCategoryId(), "-"))
                .title(inquiry.getTitle())
                .writerLabel("회원 #" + inquiry.getUserId())
                .status(inquiry.getStatus())
                .statusLabel(toStatusLabel(inquiry.getStatus()))
                .createdAt(inquiry.getCreatedAt().format(DATE_FORMAT))
                .build();
    }

    private String toStatusLabel(String status) {
        return STATUS_ANSWERED.equals(status) ? "답변완료" : "미답변";
    }

    private Specification<Inquiry> categoryEquals(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("inquiryCategoryId"), categoryId);
    }

    /** 목록 화면 필터: 프론트에서 넘어오는 PENDING/ANSWERED 값을 실제 DB 상태값으로 변환 */
    private Specification<Inquiry> statusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return (root, query, cb) -> null;
        }
        if (STATUS_ANSWERED.equalsIgnoreCase(status) || "ANSWERED".equalsIgnoreCase(status)) {
            return (root, query, cb) -> cb.equal(root.get("status"), STATUS_ANSWERED);
        }
        // "PENDING" 또는 그 외 값은 미답변(접수/처리중)으로 취급
        return statusIn(PENDING_STATUSES);
    }

    private Specification<Inquiry> statusIn(List<String> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private Specification<Inquiry> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String like = "%" + keyword.trim() + "%";
            Predicate titleLike = cb.like(root.get("title"), like);
            Predicate contentLike = cb.like(root.get("content"), like);
            return cb.or(titleLike, contentLike);
        };
    }
}