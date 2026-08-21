package kr.co.firstdayproject.service.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyDetail;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyListItem;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyListView;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyReviewRequest;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyStatistics;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.company.CompanyRejectionType;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCompanyService {

    private static final int PAGE_SIZE = 10;
    private static final String COMPANY_USER_TYPE = "기업";
    private static final String NORMAL_STATUS = "정상";
    private static final String SUSPENDED_STATUS = "이용정지";
    private static final String WITHDRAWN_STATUS = "탈퇴";
    private static final String PENDING_APPROVAL = "승인대기";
    private static final String APPROVED_APPROVAL = "승인";
    private static final String REJECTED_APPROVAL = "반려";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public AdminCompanyListView getCompanyList(
            String requestedStatus,
            String requestedKeyword,
            int requestedPage
    ) {
        String selectedStatus = normalizeStatus(requestedStatus);
        String keyword = normalizeKeyword(requestedKeyword);
        int pageNumber = Math.max(requestedPage, 1) - 1;
        PageRequest pageRequest = PageRequest.of(pageNumber, PAGE_SIZE);
        Page<Company> companyPage = companyRepository.findAdminCompanies(
                selectedStatus,
                keyword,
                pageRequest
        );

        if (companyPage.getTotalPages() > 0
                && pageNumber >= companyPage.getTotalPages()) {
            pageRequest = PageRequest.of(
                    companyPage.getTotalPages() - 1,
                    PAGE_SIZE
            );
            companyPage = companyRepository.findAdminCompanies(
                    selectedStatus,
                    keyword,
                    pageRequest
            );
        }

        Map<Long, User> managers = getManagers(companyPage.getContent());
        List<AdminCompanyListItem> items = companyPage.getContent().stream()
                .map(company -> AdminCompanyListItem.from(
                        company,
                        managers.get(company.getCompanyId())
                ))
                .toList();
        Page<AdminCompanyListItem> itemPage = new PageImpl<>(
                items,
                companyPage.getPageable(),
                companyPage.getTotalElements()
        );

        return new AdminCompanyListView(
                itemPage,
                getStatistics(),
                selectedStatus,
                keyword == null ? "" : keyword
        );
    }

    public AdminCompanyDetail getCompanyDetail(Long companyId) {
        Company company = findCompany(companyId);
        User manager = userRepository
                .findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                        companyId,
                        COMPANY_USER_TYPE
                )
                .orElse(null);

        return AdminCompanyDetail.from(company, manager);
    }

    @Transactional
    public AdminCompanyDetail approveCompany(Long adminId, Long companyId) {
        validateAdmin(adminId);
        Company company = findCompany(companyId);
        validatePendingReview(company);
        LocalDateTime now = LocalDateTime.now();

        company.setApprovalStatus(APPROVED_APPROVAL);
        company.setLatestRejectionCode(null);
        company.setLatestRejectionReason(null);
        company.setReviewedBy(adminId);
        company.setReviewedAt(now);
        company.setUpdatedAt(now);

        return toDetail(company);
    }

    @Transactional
    public AdminCompanyDetail rejectCompany(
            Long adminId,
            Long companyId,
            AdminCompanyReviewRequest request
    ) {
        validateAdmin(adminId);
        CompanyRejectionType rejectionType = validateReviewRequest(request);
        Company company = findCompany(companyId);
        validatePendingReview(company);
        LocalDateTime now = LocalDateTime.now();

        company.setApprovalStatus(REJECTED_APPROVAL);
        company.setLatestRejectionCode(rejectionType.name());
        company.setLatestRejectionReason(request.message().trim());
        company.setReviewedBy(adminId);
        company.setReviewedAt(now);
        company.setUpdatedAt(now);

        return toDetail(company);
    }

    @Transactional
    public AdminCompanyDetail suspendCompany(Long companyId) {
        Company company = findCompany(companyId);
        if (WITHDRAWN_STATUS.equals(company.getCompanyStatus())) {
            throw new IllegalStateException("탈퇴한 기업은 이용정지할 수 없습니다.");
        }
        if (!APPROVED_APPROVAL.equals(company.getApprovalStatus())
                || !NORMAL_STATUS.equals(company.getCompanyStatus())) {
            throw new IllegalStateException(
                    "승인된 정상 기업만 이용정지할 수 있습니다."
            );
        }

        List<User> companyUsers = findCompanyUsers(companyId);
        LocalDateTime now = LocalDateTime.now();
        company.setCompanyStatus(SUSPENDED_STATUS);
        company.setUpdatedAt(now);
        companyUsers.forEach(user -> {
            if (!WITHDRAWN_STATUS.equals(user.getAccountStatus())) {
                user.setAccountStatus(SUSPENDED_STATUS);
                user.setUpdatedAt(now);
            }
        });

        return AdminCompanyDetail.from(
                company,
                companyUsers.isEmpty() ? null : companyUsers.getFirst()
        );
    }

    @Transactional
    public AdminCompanyDetail unsuspendCompany(Long companyId) {
        Company company = findCompany(companyId);
        if (!APPROVED_APPROVAL.equals(company.getApprovalStatus())
                || !SUSPENDED_STATUS.equals(company.getCompanyStatus())) {
            throw new IllegalStateException(
                    "이용정지된 승인 기업만 정지를 해제할 수 있습니다."
            );
        }

        List<User> companyUsers = findCompanyUsers(companyId);
        LocalDateTime now = LocalDateTime.now();
        company.setCompanyStatus(NORMAL_STATUS);
        company.setUpdatedAt(now);
        companyUsers.forEach(user -> {
            if (SUSPENDED_STATUS.equals(user.getAccountStatus())) {
                user.setAccountStatus(NORMAL_STATUS);
                user.setUpdatedAt(now);
            }
        });

        return AdminCompanyDetail.from(
                company,
                companyUsers.isEmpty() ? null : companyUsers.getFirst()
        );
    }

    private AdminCompanyStatistics getStatistics() {
        // 심사를 요청한 기업만 센다. 가입 후 기업정보를 작성 중인 기업은 심사 대상이 아니다.
        long pendingCount = companyRepository
                .countByApprovalStatusAndCompanyStatusAndReviewRequestedAtIsNotNull(
                        PENDING_APPROVAL,
                        NORMAL_STATUS
                );
        long newReviewCount = companyRepository
                .countByApprovalStatusAndCompanyStatusAndReviewRequestedAtIsNotNullAndReapplyRequestedAtIsNull(
                        PENDING_APPROVAL,
                        NORMAL_STATUS
                );
        long reReviewCount = companyRepository
                .countByApprovalStatusAndCompanyStatusAndReviewRequestedAtIsNotNullAndReapplyRequestedAtIsNotNull(
                        PENDING_APPROVAL,
                        NORMAL_STATUS
                );
        long draftCount = companyRepository
                .countByApprovalStatusAndCompanyStatusAndReviewRequestedAtIsNull(
                        PENDING_APPROVAL,
                        NORMAL_STATUS
                );
        long approvedCount = companyRepository
                .countByApprovalStatusAndCompanyStatus(
                        APPROVED_APPROVAL,
                        NORMAL_STATUS
                );
        long rejectedCount = companyRepository
                .countByApprovalStatusAndCompanyStatus(
                        REJECTED_APPROVAL,
                        NORMAL_STATUS
                );

        return new AdminCompanyStatistics(
                pendingCount,
                newReviewCount,
                reReviewCount,
                draftCount,
                approvedCount,
                rejectedCount
        );
    }

    private Map<Long, User> getManagers(List<Company> companies) {
        List<Long> companyIds = companies.stream()
                .map(Company::getCompanyId)
                .toList();
        if (companyIds.isEmpty()) {
            return Map.of();
        }

        return userRepository
                .findByCompanyIdInAndUserTypeOrderByUserIdAsc(
                        companyIds,
                        COMPANY_USER_TYPE
                )
                .stream()
                .collect(Collectors.toMap(
                        User::getCompanyId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "기업을 찾을 수 없습니다. companyId=" + companyId
                ));
    }

    private List<User> findCompanyUsers(Long companyId) {
        return userRepository.findByCompanyIdAndUserTypeOrderByUserIdAsc(
                companyId,
                COMPANY_USER_TYPE
        );
    }

    private AdminCompanyDetail toDetail(Company company) {
        User manager = userRepository
                .findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                        company.getCompanyId(),
                        COMPANY_USER_TYPE
                )
                .orElse(null);
        return AdminCompanyDetail.from(company, manager);
    }

    private void validateAdmin(Long adminId) {
        if (adminId == null) {
            throw new IllegalStateException("관리자 인증 정보를 확인할 수 없습니다.");
        }
    }

    private void validatePendingReview(Company company) {
        if (WITHDRAWN_STATUS.equals(company.getCompanyStatus())) {
            throw new IllegalStateException("탈퇴한 기업은 심사할 수 없습니다.");
        }
        if (!NORMAL_STATUS.equals(company.getCompanyStatus())
                || !PENDING_APPROVAL.equals(company.getApprovalStatus())) {
            throw new IllegalStateException(
                    "승인 대기 중인 정상 기업만 심사할 수 있습니다."
            );
        }
        /*
         * 목록의 'ALL' 필터에는 작성 중인 기업도 보인다. 거기서 상세로 들어가 심사하는 것을 막는다.
         * 기업이 직접 심사를 요청하기 전에는 대표자명·기업소개 등이 비어 있어 심사할 내용이 없다.
         */
        if (company.getReviewRequestedAt() == null) {
            throw new IllegalStateException(
                    "아직 심사를 요청하지 않은 기업입니다. 기업이 기업정보 작성을 마치면 심사할 수 있습니다."
            );
        }
    }

    private CompanyRejectionType validateReviewRequest(
            AdminCompanyReviewRequest request
    ) {
        if (request == null) {
            throw new AdminCompanyReviewException("반려 정보를 입력해주세요.");
        }

        CompanyRejectionType rejectionType = CompanyRejectionType
                .fromCode(request.rejectionCode())
                .orElseThrow(() -> new AdminCompanyReviewException(
                        "올바른 반려 사유를 선택해주세요."
                ));
        String message = request.message();
        if (message == null || message.isBlank()) {
            throw new AdminCompanyReviewException(
                    "기업회원에게 전달할 관리자 안내를 입력해주세요."
            );
        }
        if (message.trim().length() > 1000) {
            throw new AdminCompanyReviewException(
                    "관리자 안내는 1000자 이하로 입력해주세요."
            );
        }
        return rejectionType;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "DRAFT", "PENDING", "APPROVED", "REJECTED", "SUSPENDED", "WITHDRAWN" ->
                    normalized;
            default -> "PENDING";
        };
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
