package kr.co.firstdayproject.dto.admin.company;

import org.springframework.data.domain.Page;

public record AdminCompanyListView(
        Page<AdminCompanyListItem> companyPage,
        AdminCompanyStatistics statistics,
        String selectedStatus,
        String keyword
) {
}
