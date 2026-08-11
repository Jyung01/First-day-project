package kr.co.firstdayproject.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.co.firstdayproject.dao.admin.AdminSalaryDao;
import kr.co.firstdayproject.dto.salary.SalaryJobStatDTO;
import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSalaryService {
    private static final int PAGE_SIZE = 10;
    private static final Set<String> STATUSES = Set.of("all", "정상", "숨김", "삭제");
    private static final Set<String> CAREERS = Set.of("", "newcomer", "junior", "middle", "senior");
    private static final Set<String> SORTS = Set.of("latest", "oldest", "salaryHigh", "salaryLow", "company");
    private final AdminSalaryDao adminSalaryDao;

    public Map<String, Object> getList(int requestedPage, String status, Long companyId,
                                       Long jobCategoryId, String career, String keyword, String sort) {
        status = STATUSES.contains(status) ? status : "all";
        career = CAREERS.contains(career) ? career : "";
        sort = SORTS.contains(sort) ? sort : "latest";
        keyword = keyword == null ? "" : keyword.trim();

        Map<String, Object> search = new HashMap<>();
        search.put("status", status);
        search.put("companyId", companyId);
        search.put("jobCategoryId", jobCategoryId);
        search.put("career", career);
        search.put("keyword", keyword);
        search.put("sort", sort);

        int total = adminSalaryDao.selectAdminSalaryCount(search);
        int lastPage = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        int page = Math.min(Math.max(requestedPage, 1), lastPage);
        PageHandler pageHandler = new PageHandler(page, total, PAGE_SIZE);
        search.put("offset", pageHandler.getOffset());
        search.put("pageSize", PAGE_SIZE);

        Map<String, Object> result = new HashMap<>();
        result.put("salaryList", adminSalaryDao.selectAdminSalaryList(search));
        result.put("pageHandler", pageHandler);
        result.put("summary", adminSalaryDao.selectAdminSalarySummary());
        result.put("companies", adminSalaryDao.selectCompanyOptions());
        result.put("jobs", adminSalaryDao.selectJobOptions());
        result.put("status", status);
        result.put("companyId", companyId);
        result.put("jobCategoryId", jobCategoryId);
        result.put("career", career);
        result.put("keyword", keyword);
        result.put("sort", sort);
        return result;
    }

    public SalaryRecordsDTO getDetail(Long salaryRecordId) {
        if (salaryRecordId == null) throw new IllegalArgumentException("연봉정보 번호가 필요합니다.");
        SalaryRecordsDTO detail = adminSalaryDao.selectAdminSalaryDetail(salaryRecordId);
        if (detail == null) throw new IllegalArgumentException("연봉정보를 찾을 수 없습니다.");
        return detail;
    }

    @Transactional
    public void review(Long adminId, Long salaryRecordId, String status, String hiddenReason) {
        if (!Set.of("정상", "숨김").contains(status))
            throw new IllegalArgumentException("올바른 검토 결과를 선택해주세요.");
        hiddenReason = hiddenReason == null ? "" : hiddenReason.trim();
        if ("숨김".equals(status) && hiddenReason.isBlank())
            throw new IllegalArgumentException("숨김 사유를 입력해주세요.");
        if (hiddenReason.length() > 1000)
            throw new IllegalArgumentException("숨김 사유는 1000자 이내로 입력해주세요.");
        getDetail(salaryRecordId);
        if (adminSalaryDao.updateSalaryStatus(salaryRecordId, status, hiddenReason, adminId) != 1)
            throw new IllegalStateException("연봉정보 상태를 저장하지 못했습니다.");
    }
}
