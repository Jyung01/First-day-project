package kr.co.firstdayproject.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.admin.member.AdminMemberDetail;
import kr.co.firstdayproject.dto.admin.member.AdminMemberListView;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AdminMemberServiceTest {

    @Test
    void suspendsActivePersonalMember() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User member = User.builder()
                .userId(12L)
                .userType("개인")
                .accountStatus("정상")
                .build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(member));

        AdminMemberDetail result = service.suspendMember(12L);

        assertThat(member.getAccountStatus()).isEqualTo("이용정지");
        assertThat(result.statusCode()).isEqualTo("SUSPENDED");
    }

    @Test
    void unsuspendsSuspendedPersonalMember() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User member = User.builder()
                .userId(12L)
                .userType("개인")
                .accountStatus("이용정지")
                .build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(member));

        AdminMemberDetail result = service.unsuspendMember(12L);

        assertThat(member.getAccountStatus()).isEqualTo("정상");
        assertThat(result.statusCode()).isEqualTo("ACTIVE");
    }

    @Test
    void refusesToChangeWithdrawnMemberStatus() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User withdrawnMember = User.builder()
                .userId(12L)
                .userType("개인")
                .accountStatus("탈퇴")
                .build();
        when(userRepository.findById(12L))
                .thenReturn(Optional.of(withdrawnMember));

        assertThatThrownBy(() -> service.suspendMember(12L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("탈퇴");
    }

    @Test
    void returnsPersonalMemberDetail() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User member = User.builder()
                .userId(12L)
                .loginId("member12")
                .name("김민준")
                .email("member12@example.com")
                .phone("010-1234-5678")
                .userType("개인")
                .accountStatus("정상")
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .lastLoginAt(LocalDateTime.of(2026, 8, 10, 9, 20))
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 20))
                .build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(member));

        AdminMemberDetail result = service.getMemberDetail(12L);

        assertThat(result.userId()).isEqualTo(12L);
        assertThat(result.loginId()).isEqualTo("member12");
        assertThat(result.phone()).isEqualTo("010-1234-5678");
        assertThat(result.statusCode()).isEqualTo("ACTIVE");
        assertThat(result.lastLoginAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 9, 20));
    }

    @Test
    void doesNotExposeNonPersonalAccountAsMemberDetail() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User companyUser = User.builder()
                .userId(20L)
                .userType("기업")
                .build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(companyUser));

        assertThatThrownBy(() -> service.getMemberDetail(20L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsFilteredPersonalMembersAndStatistics() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        User suspendedMember = User.builder()
                .userId(12L)
                .loginId("member12")
                .name("김민준")
                .email("member12@example.com")
                .phone("010-1234-5678")
                .userType("개인")
                .accountStatus("이용정지")
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .build();
        Page<User> repositoryPage = new PageImpl<>(
                List.of(suspendedMember),
                PageRequest.of(1, 10),
                21
        );
        when(userRepository.findAdminMembers(
                eq("개인"),
                eq(List.of("이용정지")),
                eq("김민준"),
                any(Pageable.class)
        )).thenReturn(repositoryPage);
        when(userRepository.countByUserType("개인")).thenReturn(30L);
        when(userRepository.countByUserTypeAndCreatedAtBetween(
                eq("개인"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(2L);
        when(userRepository.countByUserTypeAndAccountStatus("개인", "이용정지"))
                .thenReturn(4L);

        AdminMemberListView result = service.getMemberList(
                "suspended",
                "  김민준  ",
                2
        );

        assertThat(result.selectedStatus()).isEqualTo("SUSPENDED");
        assertThat(result.keyword()).isEqualTo("김민준");
        assertThat(result.memberPage().getContent()).hasSize(1);
        assertThat(result.memberPage().getContent().getFirst().statusCode())
                .isEqualTo("SUSPENDED");
        assertThat(result.statistics().totalCount()).isEqualTo(30L);
        assertThat(result.statistics().todayCount()).isEqualTo(2L);
        assertThat(result.statistics().suspendedCount()).isEqualTo(4L);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAdminMembers(
                eq("개인"),
                eq(List.of("이용정지")),
                eq("김민준"),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull();
    }

    @Test
    void excludesWithdrawnMembersFromAllStatus() {
        UserRepository userRepository = mock(UserRepository.class);
        AdminMemberService service = new AdminMemberService(userRepository);
        when(userRepository.findAdminMembers(
                eq("개인"),
                eq(List.of("정상", "이용정지")),
                isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        AdminMemberListView result = service.getMemberList("ALL", null, 1);

        assertThat(result.selectedStatus()).isEqualTo("ALL");
        verify(userRepository).findAdminMembers(
                eq("개인"),
                eq(List.of("정상", "이용정지")),
                isNull(),
                any(Pageable.class)
        );
    }
}
