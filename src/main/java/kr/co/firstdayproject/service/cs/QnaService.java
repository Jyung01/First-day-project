package kr.co.firstdayproject.service.cs;

import jakarta.persistence.criteria.Predicate;
import kr.co.firstdayproject.dto.admin.InquiryDetailDto;
import kr.co.firstdayproject.dto.admin.InquiryListItemDto;
import kr.co.firstdayproject.entity.cs.Inquiry;
import kr.co.firstdayproject.entity.cs.InquiryAttachment;
import kr.co.firstdayproject.entity.cs.InquiryCategory;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.cs.InquiryAttachmentRepository;
import kr.co.firstdayproject.repository.cs.InquiryCategoryRepository;
import kr.co.firstdayproject.repository.cs.InquiryRepository;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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

    private final InquiryRepository inquiryRepository;
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AwsS3Service awsS3Service;

    /** 노출용 활성 카테고리 목록 (등록순) */
    public List<InquiryCategory> getActiveCategories() {
        return inquiryCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /** 관리자 대시보드 - 미답변 문의 건수 */
    public long getPendingCount() {
        return inquiryRepository.count(statusIn(PENDING_STATUSES));
    }

    /**
     * 관리자 대시보드 - 접수된 지 24시간이 지나도록 답변되지 않은 문의 건수.
     * 미답변 건수만으로는 방금 들어온 문의와 오래 밀린 문의를 구분할 수 없어 따로 센다.
     */
    public long getPendingOverDayCount() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(24);

        Specification<Inquiry> spec = Specification
                .where(statusIn(PENDING_STATUSES))
                .and((root, query, cb) -> cb.lessThan(root.get("createdAt"), deadline));

        return inquiryRepository.count(spec);
    }

    /** 관리자 대시보드 - 오늘 답변 완료된 문의 건수 */
    public long getTodayAnsweredCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Specification<Inquiry> spec = Specification
                .where(statusIn(List.of(STATUS_ANSWERED)))
                .and((root, query, cb) -> cb.between(root.get("answeredAt"), start, end));

        return inquiryRepository.count(spec);
    }

    /** 사용자 - 내 1:1 문의 목록 조회 (회원 본인 문의만) */
    public Page<InquiryListItemDto> getMyInquiryList(Long userId, String status, Pageable pageable) {
        Map<Long, String> categoryNameMap = getActiveCategories().stream()
                .collect(Collectors.toMap(InquiryCategory::getInquiryCategoryId, InquiryCategory::getCategoryName));

        Specification<Inquiry> spec = Specification.where(userIdEquals(userId))
                .and(statusFilter(status));

        Page<Inquiry> inquiries = inquiryRepository.findAll(spec, latestFirst(pageable));

        Map<Long, User> userMap = getUserMap(inquiries);
        Map<Long, Company> companyMap = getCompanyMap(userMap);
        return inquiries.map(inquiry -> toListItem(
                inquiry,
                categoryNameMap,
                userMap,
                companyMap
        ));
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

    /** 첨부파일을 비공개 S3 버킷에 저장하고 메타데이터를 생성합니다. */
    private void saveAttachments(Long inquiryId, List<MultipartFile> files) {
        List<InquiryAttachment> attachments = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            String storageKey;

            try {
                storageKey = awsS3Service.uploadPrivate(file, "qna/" + inquiryId);
            } catch (IOException e) {
                throw new IllegalStateException("첨부파일 저장에 실패했습니다: " + originalName, e);
            }
            awsS3Service.synchronizePrivateReplacement(null, storageKey);

            InquiryAttachment attachment = InquiryAttachment.builder()
                    .inquiryId(inquiryId)
                    .originalName(originalName)
                    .storageKey(storageKey)
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

    /** 관리자 - 문의 첨부파일 조회 (다운로드용) */
    @Transactional(readOnly = true)
    public InquiryAttachment getAttachmentForAdmin(Long attachmentId) {
        return inquiryAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "첨부파일을 찾을 수 없습니다. id=" + attachmentId
                ));
    }

    /** S3에 저장된 첨부파일의 임시 다운로드 URL을 발급합니다. */
    public String getAttachmentDownloadUrl(InquiryAttachment attachment) {
        if (isLegacyLocalFile(attachment.getStorageKey())) {
            return null;
        }
        return awsS3Service.getPresignedDownloadUrl(
                attachment.getStorageKey(),
                attachment.getOriginalName()
        );
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
                deleteAttachmentFile(attachment);
            }
            inquiryAttachmentRepository.deleteAll(attachments);
        }

        inquiryRepository.delete(inquiry);
    }

    private void deleteAttachmentFile(InquiryAttachment attachment) {
        String storageKey = attachment.getStorageKey();
        if (isLegacyLocalFile(storageKey)) {
            try {
                Files.deleteIfExists(Path.of(storageKey));
            } catch (IOException ignored) {
                // 레거시 로컬 파일 삭제 실패가 문의 삭제를 막지 않게 합니다.
            }
            return;
        }
        awsS3Service.deletePrivate(storageKey);
    }

    private boolean isLegacyLocalFile(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return false;
        }
        try {
            return Path.of(storageKey).isAbsolute();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** 관리자 - 1:1 문의 목록 검색/페이징 */
    public Page<InquiryListItemDto> getInquiryList(
            Long categoryId,
            String memberType,
            String status,
            String keyword,
            Pageable pageable
    ) {
        Map<Long, String> categoryNameMap = getActiveCategories().stream()
                .collect(Collectors.toMap(InquiryCategory::getInquiryCategoryId, InquiryCategory::getCategoryName));

        Specification<Inquiry> spec = Specification.where(categoryEquals(categoryId))
                .and(userTypeEquals(memberType))
                .and(statusFilter(status))
                .and(keywordContains(keyword));

        Page<Inquiry> inquiries = inquiryRepository.findAll(spec, latestFirst(pageable));

        Map<Long, User> userMap = getUserMap(inquiries);
        Map<Long, Company> companyMap = getCompanyMap(userMap);
        return inquiries.map(inquiry -> toListItem(
                inquiry,
                categoryNameMap,
                userMap,
                companyMap
        ));
    }

    private Pageable latestFirst(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("inquiryId"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    /** 관리자 - 문의 상세 (답변 모달 데이터) */
    public InquiryDetailDto getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        String categoryName = inquiryCategoryRepository.findById(inquiry.getInquiryCategoryId())
                .map(InquiryCategory::getCategoryName)
                .orElse("-");

        List<InquiryAttachment> attachments = inquiryAttachmentRepository.findByInquiryId(inquiryId);
        User writer = userRepository.findById(inquiry.getUserId()).orElse(null);
        Company company = writer != null && writer.getCompanyId() != null
                ? companyRepository.findById(writer.getCompanyId()).orElse(null)
                : null;

        return InquiryDetailDto.builder()
                .inquiryId(inquiry.getInquiryId())
                .categoryName(categoryName)
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .writerLabel(toWriterLabel(inquiry.getUserId(), writer, company))
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
    public void updateAnswer(Long inquiryId, String answerContent, Long adminId) {
        validateAnswerContent(answerContent);

        Inquiry inquiry = getInquiryOrThrow(inquiryId);

        if (!STATUS_ANSWERED.equals(inquiry.getStatus())) {
            throw new IllegalStateException("아직 답변이 등록되지 않은 문의입니다.");
        }

        // answeredBy/answeredAt은 "최종 답변자와 그 시각"으로 본다.
        // 답변을 고친 사람이 현재 답변의 책임자이므로 둘을 함께 갱신한다.
        // 한쪽만 바꾸면 "A가 답변했는데 시각은 B가 고친 때"처럼 어긋난다.
        LocalDateTime now = LocalDateTime.now();
        inquiry.setAnswerContent(answerContent.trim());
        inquiry.setAnsweredBy(adminId);
        inquiry.setAnsweredAt(now);
        inquiry.setUpdatedAt(now);
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

    private InquiryListItemDto toListItem(
            Inquiry inquiry,
            Map<Long, String> categoryNameMap,
            Map<Long, User> userMap,
            Map<Long, Company> companyMap
    ) {
        User writer = userMap.get(inquiry.getUserId());
        return InquiryListItemDto.builder()
                .inquiryId(inquiry.getInquiryId())
                .categoryName(categoryNameMap.getOrDefault(inquiry.getInquiryCategoryId(), "-"))
                .memberType(toMemberTypeLabel(writer))
                .title(inquiry.getTitle())
                .writerLabel(toWriterName(inquiry.getUserId(), writer, companyMap))
                .status(inquiry.getStatus())
                .statusLabel(toStatusLabel(inquiry.getStatus()))
                .createdAt(inquiry.getCreatedAt().format(DATE_FORMAT))
                .build();
    }

    private Map<Long, User> getUserMap(Page<Inquiry> inquiries) {
        List<Long> userIds = inquiries.getContent().stream()
                .map(Inquiry::getUserId)
                .distinct()
                .toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
    }

    private Map<Long, Company> getCompanyMap(Map<Long, User> userMap) {
        List<Long> companyIds = userMap.values().stream()
                .map(User::getCompanyId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getCompanyId, company -> company));
    }

    private String toMemberTypeLabel(User user) {
        if (user == null) {
            return "-";
        }
        return switch (user.getUserType()) {
            case "개인" -> "개인회원";
            case "기업" -> "기업회원";
            default -> user.getUserType();
        };
    }

    private String toWriterName(
            Long userId,
            User writer,
            Map<Long, Company> companyMap
    ) {
        if (writer == null) {
            return "회원 #" + userId;
        }
        if ("기업".equals(writer.getUserType()) && writer.getCompanyId() != null) {
            Company company = companyMap.get(writer.getCompanyId());
            if (company != null) {
                return company.getCompanyName();
            }
        }
        return writer.getName();
    }

    private String toWriterLabel(Long userId, User writer, Company company) {
        if (writer == null) {
            return "회원 #" + userId;
        }
        if ("기업".equals(writer.getUserType()) && company != null) {
            return company.getCompanyName()
                    + " · 담당자 " + writer.getName()
                    + " · 기업회원";
        }
        return writer.getName() + " · " + toMemberTypeLabel(writer);
    }

    private String toStatusLabel(String status) {
        return STATUS_ANSWERED.equals(status) ? "답변완료" : "미답변";
    }

    private Specification<Inquiry> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private Specification<Inquiry> userTypeEquals(String memberType) {
        if (!StringUtils.hasText(memberType)) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Long> userIds = userRepository.findUserIdsByUserType(memberType);
        return (root, query, cb) -> userIds.isEmpty()
                ? cb.disjunction()
                : root.get("userId").in(userIds);
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
