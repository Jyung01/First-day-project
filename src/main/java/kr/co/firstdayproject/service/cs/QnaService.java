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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
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

    /** 첨부파일 정책 */
    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");

    /**
     * 첨부파일 저장 루트 경로.
     * application.yml/properties에 cs.qna.upload-dir 설정이 없으면 기본값(./uploads/qna)을 사용합니다.
     * 운영 환경(예: S3, 별도 스토리지 서버)을 쓰는 경우 이 부분을 해당 스토리지 클라이언트 호출로 교체하면 됩니다.
     */
    @Value("${cs.qna.upload-dir:uploads/qna}")
    private String uploadDir;

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

    /** 사용자 - 내 1:1 문의 목록 조회 (회원 본인 문의만) */
    public Page<InquiryListItemDto> getMyInquiryList(Long userId, String status, Pageable pageable) {
        Map<Long, String> categoryNameMap = getActiveCategories().stream()
                .collect(Collectors.toMap(InquiryCategory::getInquiryCategoryId, InquiryCategory::getCategoryName));

        Specification<Inquiry> spec = Specification.where(userIdEquals(userId))
                .and(statusFilter(status));

        Page<Inquiry> inquiries = inquiryRepository.findAll(spec, pageable);

        return inquiries.map(inquiry -> toListItem(inquiry, categoryNameMap));
    }

    /** 사용자 - 문의 상세가 본인 것인지 검증 후 조회 */
    public InquiryDetailDto getMyInquiryDetail(Long inquiryId, Long userId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        if (!inquiry.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 문의만 조회할 수 있습니다.");
        }
        return getInquiryDetail(inquiryId);
    }

    /** 사용자 - 문의 등록 (개인/기업회원 공통), 첨부파일 0~3개 포함 */
    @Transactional
    public Long createInquiry(Long userId, Long categoryId, String title, String content, List<MultipartFile> files) {
        validateCreateInquiry(categoryId, title, content);
        List<MultipartFile> validFiles = validateAndFilterFiles(files);

        Inquiry inquiry = Inquiry.builder()
                .userId(userId)
                .inquiryCategoryId(categoryId)
                .title(title.trim())
                .content(content.trim())
                .status(STATUS_RECEIVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        inquiryRepository.save(inquiry);

        if (!validFiles.isEmpty()) {
            saveAttachments(inquiry.getInquiryId(), validFiles);
        }

        return inquiry.getInquiryId();
    }

    private void validateCreateInquiry(Long categoryId, String title, String content) {
        if (categoryId == null) {
            throw new IllegalArgumentException("문의 유형을 선택해주세요.");
        }
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (title.trim().length() > 100) {
            throw new IllegalArgumentException("제목은 100자 이내로 입력해주세요.");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("문의 내용을 입력해주세요.");
        }
        if (content.trim().length() > 1000) {
            throw new IllegalArgumentException("문의 내용은 1000자 이내로 입력해주세요.");
        }
    }

    /** 첨부파일 개수/용량/확장자 검증 후 빈 파일을 제외한 목록 반환 */
    private List<MultipartFile> validateAndFilterFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.size() > MAX_FILE_COUNT) {
            throw new IllegalArgumentException("첨부파일은 최대 " + MAX_FILE_COUNT + "개까지 등록할 수 있습니다.");
        }

        for (MultipartFile file : validFiles) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(file.getOriginalFilename() + " 파일이 10MB를 초과합니다.");
            }
            String ext = extractExtension(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("JPG, PNG, PDF 파일만 첨부할 수 있습니다.");
            }
        }

        return validFiles;
    }

    /**
     * 첨부파일을 디스크에 저장하고 InquiryAttachment 레코드를 생성합니다.
     * NOTE: InquiryAttachment/InquiryAttachmentRepository 소스를 확인하지 못해
     *       필드명(originalName, fileSize 등 기존 코드에서 쓰던 것 기준)을 추정해 작성했습니다.
     *       실제 엔티티의 빌더 필드명이 다르면 이 메서드만 맞춰 수정해주세요.
     */
    private void saveAttachments(Long inquiryId, List<MultipartFile> files) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new UncheckedIOException("첨부파일 저장 폴더 생성에 실패했습니다.", e);
        }

        List<InquiryAttachment> attachments = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            String ext = extractExtension(originalName);
            String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            Path targetPath = uploadPath.resolve(storedName);

            try {
                file.transferTo(targetPath.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException("첨부파일 저장에 실패했습니다: " + originalName, e);
            }

            // NOTE: storageKey는 엔티티 설명상 "객체 스토리지" 키를 의도한 필드입니다.
            //       현재는 별도 스토리지 연동 전이라 로컬 디스크 절대경로를 그대로 저장합니다.
            //       추후 S3 등으로 전환 시 이 값을 버킷 키(예: "qna/{storedName}")로 바꾸고
            //       업로드 로직도 해당 SDK 호출로 교체하면 됩니다.
            InquiryAttachment attachment = InquiryAttachment.builder()
                    .inquiryId(inquiryId)
                    .originalName(originalName)
                    .storageKey(targetPath.toString())
                    .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .fileSize(file.getSize())
                    .createdAt(LocalDateTime.now())
                    .build();

            attachments.add(attachment);
        }

        inquiryAttachmentRepository.saveAll(attachments);
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 사용자 - 첨부파일이 본인 문의의 것인지 검증 후 반환 (다운로드용) */
    public InquiryAttachment getMyAttachment(Long attachmentId, Long userId) {
        InquiryAttachment attachment = inquiryAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException("첨부파일을 찾을 수 없습니다. id=" + attachmentId));

        Inquiry inquiry = getInquiryOrThrow(attachment.getInquiryId());
        if (!inquiry.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 문의의 첨부파일만 다운로드할 수 있습니다.");
        }

        return attachment;
    }

    /** 사용자 - 문의 삭제 (본인 문의 + 답변대기 상태만 가능) */
    @Transactional
    public void deleteMyInquiry(Long inquiryId, Long userId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        if (!inquiry.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 문의만 삭제할 수 있습니다.");
        }
        if (STATUS_ANSWERED.equals(inquiry.getStatus())) {
            throw new IllegalStateException("답변완료된 문의는 삭제할 수 없습니다.");
        }

        List<InquiryAttachment> attachments = inquiryAttachmentRepository.findByInquiryId(inquiryId);
        if (!attachments.isEmpty()) {
            for (InquiryAttachment attachment : attachments) {
                try {
                    Files.deleteIfExists(Path.of(attachment.getStorageKey()));
                } catch (IOException e) {
                    // 파일 삭제 실패는 문의 삭제 자체를 막지 않고 무시합니다. 필요 시 로깅 추가.
                }
            }
            inquiryAttachmentRepository.deleteAll(attachments);
        }

        inquiryRepository.delete(inquiry);
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

    private Specification<Inquiry> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
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