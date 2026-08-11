package kr.co.firstdayproject.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyDetail;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyListView;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyReviewRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AdminCompanyServiceTest {

    @Test
    void returnsFilteredCompaniesWithManagersAndStatistics() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인대기", "정상");
        company.setReapplyRequestedAt(LocalDateTime.of(2026, 8, 10, 9, 30));
        User manager = User.builder()
                .userId(20L)
                .companyId(12L)
                .name("김담당")
                .userType("기업")
                .build();
        Page<Company> repositoryPage = new PageImpl<>(
                List.of(company),
                PageRequest.of(0, 10),
                1
        );
        when(companyRepository.findAdminCompanies(
                eq("PENDING"),
                eq("코드"),
                any(Pageable.class)
        )).thenReturn(repositoryPage);
        when(userRepository.findByCompanyIdInAndUserTypeOrderByUserIdAsc(
                List.of(12L),
                "기업"
        )).thenReturn(List.of(manager));
        stubStatistics(companyRepository);

        AdminCompanyListView result = service.getCompanyList(
                "pending",
                "  코드  ",
                1
        );

        assertThat(result.selectedStatus()).isEqualTo("PENDING");
        assertThat(result.keyword()).isEqualTo("코드");
        assertThat(result.companyPage().getContent()).hasSize(1);
        assertThat(result.companyPage().getContent().getFirst().managerName())
                .isEqualTo("김담당");
        assertThat(result.companyPage().getContent().getFirst().reviewTypeCode())
                .isEqualTo("REVIEW");
        assertThat(result.statistics().pendingCount()).isEqualTo(6L);
        assertThat(result.statistics().newReviewCount()).isEqualTo(4L);
        assertThat(result.statistics().reReviewCount()).isEqualTo(2L);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(companyRepository).findAdminCompanies(
                eq("PENDING"),
                eq("코드"),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void returnsRejectedCompanyDetailWithManagerAndReason() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "반려", "정상");
        company.setRepresentativeName("김대표");
        company.setEstablishedDate(LocalDate.of(2020, 3, 1));
        company.setLatestRejectionCode("FORMAT_ERROR");
        company.setLatestRejectionReason("사업자등록번호를 확인해주세요.");
        company.setBenefits("[\"유연근무제\",\"교육비 지원\"]");
        User manager = User.builder()
                .userId(20L)
                .companyId(12L)
                .name("김담당")
                .email("manager@example.com")
                .phone("010-1234-5678")
                .userType("기업")
                .build();
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(Optional.of(manager));

        AdminCompanyDetail result = service.getCompanyDetail(12L);

        assertThat(result.companyNumber()).isEqualTo("C-0012");
        assertThat(result.businessNumber()).isEqualTo("123-45-67890");
        assertThat(result.statusCode()).isEqualTo("REJECTED");
        assertThat(result.rejectionLabel()).isEqualTo("형식 오류");
        assertThat(result.rejectionReason())
                .isEqualTo("사업자등록번호를 확인해주세요.");
        assertThat(result.benefits()).isEqualTo("유연근무제 · 교육비 지원");
        assertThat(result.managerEmail()).isEqualTo("manager@example.com");
    }

    @Test
    void mapsWithdrawnCompanyBeforeApprovalStatus() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인", "탈퇴");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(Optional.empty());

        AdminCompanyDetail result = service.getCompanyDetail(12L);

        assertThat(result.statusCode()).isEqualTo("WITHDRAWN");
        assertThat(result.statusLabel()).isEqualTo("탈퇴");
        assertThat(result.reviewTypeLabel()).isEqualTo("-");
        assertThat(result.managerName()).isEqualTo("미등록");
    }

    @Test
    void hidesReviewTypeForApprovedCompany() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인", "정상");
        company.setReapplyRequestedAt(LocalDateTime.of(2026, 8, 5, 9, 0));
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(Optional.empty());

        AdminCompanyDetail result = service.getCompanyDetail(12L);

        assertThat(result.reviewTypeCode()).isEqualTo("REVIEW");
        assertThat(result.reviewTypeLabel()).isEqualTo("-");
    }

    @Test
    void refusesMissingCompanyDetail() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompanyDetail(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void approvesPendingCompanyAndClearsPreviousRejection() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인대기", "정상");
        company.setLatestRejectionCode("MISSING_INFORMATION");
        company.setLatestRejectionReason("필수 정보를 확인해주세요.");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(Optional.empty());

        AdminCompanyDetail result = service.approveCompany(99L, 12L);

        assertThat(company.getApprovalStatus()).isEqualTo("승인");
        assertThat(company.getLatestRejectionCode()).isNull();
        assertThat(company.getLatestRejectionReason()).isNull();
        assertThat(company.getReviewedBy()).isEqualTo(99L);
        assertThat(company.getReviewedAt()).isNotNull();
        assertThat(result.statusCode()).isEqualTo("APPROVED");
    }

    @Test
    void rejectsPendingCompanyWithValidatedReason() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인대기", "정상");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(Optional.empty());

        AdminCompanyDetail result = service.rejectCompany(
                99L,
                12L,
                new AdminCompanyReviewRequest(
                        "FORMAT_ERROR",
                        " 사업자등록번호를 확인해주세요. "
                )
        );

        assertThat(company.getApprovalStatus()).isEqualTo("반려");
        assertThat(company.getLatestRejectionCode()).isEqualTo("FORMAT_ERROR");
        assertThat(company.getLatestRejectionReason())
                .isEqualTo("사업자등록번호를 확인해주세요.");
        assertThat(company.getReviewedBy()).isEqualTo(99L);
        assertThat(result.statusCode()).isEqualTo("REJECTED");
        assertThat(result.rejectionLabel()).isEqualTo("형식 오류");
    }

    @Test
    void refusesUnknownRejectionCode() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );

        assertThatThrownBy(() -> service.rejectCompany(
                99L,
                12L,
                new AdminCompanyReviewRequest("UNKNOWN", "안내")
        )).isInstanceOf(AdminCompanyReviewException.class)
                .hasMessageContaining("반려 사유");
    }

    @Test
    void suspendsApprovedCompanyAndItsAccounts() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인", "정상");
        User manager = companyUser(20L, "정상");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(List.of(manager));

        AdminCompanyDetail result = service.suspendCompany(12L);

        assertThat(company.getCompanyStatus()).isEqualTo("이용정지");
        assertThat(manager.getAccountStatus()).isEqualTo("이용정지");
        assertThat(company.getUpdatedAt()).isNotNull();
        assertThat(manager.getUpdatedAt()).isNotNull();
        assertThat(result.statusCode()).isEqualTo("SUSPENDED");
    }

    @Test
    void unsuspendsCompanyAndItsSuspendedAccounts() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인", "이용정지");
        User manager = companyUser(20L, "이용정지");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));
        when(userRepository.findByCompanyIdAndUserTypeOrderByUserIdAsc(
                12L,
                "기업"
        )).thenReturn(List.of(manager));

        AdminCompanyDetail result = service.unsuspendCompany(12L);

        assertThat(company.getCompanyStatus()).isEqualTo("정상");
        assertThat(manager.getAccountStatus()).isEqualTo("정상");
        assertThat(result.statusCode()).isEqualTo("APPROVED");
    }

    @Test
    void refusesToChangeWithdrawnCompany() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCompanyService service = new AdminCompanyService(
                companyRepository,
                userRepository
        );
        Company company = company(12L, "승인", "탈퇴");
        when(companyRepository.findById(12L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.suspendCompany(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("탈퇴");
    }

    private Company company(
            Long companyId,
            String approvalStatus,
            String companyStatus
    ) {
        return Company.builder()
                .companyId(companyId)
                .companyName("코드웨이브")
                .businessNumber("1234567890")
                .approvalStatus(approvalStatus)
                .companyStatus(companyStatus)
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();
    }

    private User companyUser(Long userId, String accountStatus) {
        return User.builder()
                .userId(userId)
                .companyId(12L)
                .name("김담당")
                .email("manager@example.com")
                .phone("010-1234-5678")
                .userType("기업")
                .accountStatus(accountStatus)
                .build();
    }

    private void stubStatistics(CompanyRepository companyRepository) {
        when(companyRepository.countByApprovalStatusAndCompanyStatus(
                "승인대기",
                "정상"
        )).thenReturn(6L);
        when(companyRepository
                .countByApprovalStatusAndCompanyStatusAndReapplyRequestedAtIsNull(
                        "승인대기",
                        "정상"
                )).thenReturn(4L);
        when(companyRepository
                .countByApprovalStatusAndCompanyStatusAndReapplyRequestedAtIsNotNull(
                        "승인대기",
                        "정상"
                )).thenReturn(2L);
        when(companyRepository.countByApprovalStatusAndCompanyStatus(
                "승인",
                "정상"
        )).thenReturn(126L);
        when(companyRepository.countByApprovalStatusAndCompanyStatus(
                "반려",
                "정상"
        )).thenReturn(3L);
    }
}
