package kr.co.firstdayproject.dto.admin.member;

import org.springframework.data.domain.Page;

public record AdminMemberListView(
        Page<AdminMemberListItem> memberPage,
        AdminMemberStatistics statistics,
        String selectedStatus,
        String keyword
) {
}
