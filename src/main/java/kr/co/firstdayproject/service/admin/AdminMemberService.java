package kr.co.firstdayproject.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import kr.co.firstdayproject.dto.admin.member.AdminMemberDetail;
import kr.co.firstdayproject.dto.admin.member.AdminMemberListItem;
import kr.co.firstdayproject.dto.admin.member.AdminMemberListView;
import kr.co.firstdayproject.dto.admin.member.AdminMemberStatistics;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private static final int PAGE_SIZE = 10;
    private static final String PERSONAL_USER_TYPE = "개인";
    private static final String ACTIVE_STATUS = "정상";
    private static final String SUSPENDED_STATUS = "이용정지";
    private static final String WITHDRAWN_STATUS = "탈퇴";

    private final UserRepository userRepository;

    public AdminMemberDetail getMemberDetail(Long memberId) {
        return AdminMemberDetail.from(findPersonalMember(memberId));
    }

    @Transactional
    public AdminMemberDetail suspendMember(Long memberId) {
        User member = findPersonalMember(memberId);

        validateStatusChange(member, ACTIVE_STATUS, "이용정지");
        member.setAccountStatus(SUSPENDED_STATUS);

        return AdminMemberDetail.from(member);
    }

    @Transactional
    public AdminMemberDetail unsuspendMember(Long memberId) {
        User member = findPersonalMember(memberId);

        validateStatusChange(member, SUSPENDED_STATUS, "정지 해제");
        member.setAccountStatus(ACTIVE_STATUS);

        return AdminMemberDetail.from(member);
    }

    public AdminMemberListView getMemberList(
            String requestedStatus,
            String requestedKeyword,
            int requestedPage
    ) {
        String selectedStatus = normalizeStatusCode(requestedStatus);
        List<String> accountStatuses = toAccountStatuses(selectedStatus);
        String keyword = normalizeKeyword(requestedKeyword);
        int page = Math.max(requestedPage, 1) - 1;

        PageRequest pageRequest = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "userId"))
        );
        Page<User> userPage = userRepository.findAdminMembers(
                        PERSONAL_USER_TYPE,
                        accountStatuses,
                        keyword,
                        pageRequest
                );
        if (userPage.getTotalPages() > 0 && page >= userPage.getTotalPages()) {
            pageRequest = PageRequest.of(
                    userPage.getTotalPages() - 1,
                    PAGE_SIZE,
                    pageRequest.getSort()
            );
            userPage = userRepository.findAdminMembers(
                    PERSONAL_USER_TYPE,
                    accountStatuses,
                    keyword,
                    pageRequest
            );
        }
        Page<AdminMemberListItem> memberPage = userPage.map(AdminMemberListItem::from);

        return new AdminMemberListView(
                memberPage,
                getStatistics(),
                selectedStatus,
                keyword == null ? "" : keyword
        );
    }

    private AdminMemberStatistics getStatistics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        return new AdminMemberStatistics(
                userRepository.countByUserType(PERSONAL_USER_TYPE),
                userRepository.countByUserTypeAndCreatedAtBetween(
                        PERSONAL_USER_TYPE,
                        startOfToday,
                        startOfTomorrow
                ),
                userRepository.countByUserTypeAndAccountStatus(
                        PERSONAL_USER_TYPE,
                        SUSPENDED_STATUS
                )
        );
    }

    private String normalizeStatusCode(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE", "SUSPENDED", "WITHDRAWN" -> normalized;
            default -> "ALL";
        };
    }

    private List<String> toAccountStatuses(String statusCode) {
        return switch (statusCode) {
            case "ACTIVE" -> List.of("정상");
            case "SUSPENDED" -> List.of("이용정지");
            case "WITHDRAWN" -> List.of("탈퇴");
            default -> List.of("정상", "이용정지");
        };
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private User findPersonalMember(Long memberId) {
        return userRepository.findById(memberId)
                .filter(user -> PERSONAL_USER_TYPE.equals(user.getUserType()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원을 찾을 수 없습니다. memberId=" + memberId
                ));
    }

    private void validateStatusChange(
            User member,
            String requiredStatus,
            String actionName
    ) {
        if (WITHDRAWN_STATUS.equals(member.getAccountStatus())) {
            throw new IllegalStateException(
                    "탈퇴한 회원은 상태를 변경할 수 없습니다."
            );
        }
        if (!requiredStatus.equals(member.getAccountStatus())) {
            throw new IllegalStateException(
                    actionName + "할 수 없는 계정 상태입니다."
            );
        }
    }
}
