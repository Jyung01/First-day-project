package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyDao;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.dto.job.JobDTO;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {
    private final CompanyDao companyDao;

    // 메인 : 인기 기업
    public List<CompanyDTO> getPopularCompanyList() {
        return companyDao.selectPopularCompanyList();
    }

    // 기업정보 목록 :  기업 목록 조회
    public List<CompanyDTO> getCompanyList(CompanySearchDTO search) {
        return companyDao.selectCompanyList(search);
    }

    // 기업정보 목록 :  전체 기업 수
    public int getCompanyCount(CompanySearchDTO search) {
        return companyDao.selectCompanyCount(search);
    }

    // 기업정보 목록 : 검색 - 업종, 지역, 기업규모, 직무 전체
    public List<String> getIndustryList() {
        return companyDao.selectIndustryList();
    }
    public List<String> getRegionList() {
        return companyDao.selectRegionList();
    }
    public List<String> getCompanySizeList() {
        return companyDao.selectCompanySizeList();
    }
    public List<JobCategoryOption> getJobCategoryList() {
        return companyDao.selectJobCategoryList();
    }

    // 기업정보 상세
    public CompanyDTO getCompanyDetail(Long companyId) {
        CompanyDTO company = companyDao.selectCompanyDetail(companyId);
        if (company == null) {
            throw new ResourceNotFoundException(
                    "조회할 수 없는 기업정보입니다."
            );
        }
        company.setBenefitList(parseBenefits(company.getBenefits()));
        return company;
    }

    /** companies.benefits에 JSON 배열 문자열(["4대보험","교육비 지원"])로 저장된 복지 태그를 파싱 */
    private List<String> parseBenefits(String benefitsJson) {
        if (benefitsJson == null || benefitsJson.isBlank()) {
            return List.of();
        }

        String content = benefitsJson.trim();
        if (content.length() < 2 || content.charAt(0) != '[') {
            return List.of();
        }

        return Arrays.stream(
                        content.substring(1, content.length() - 1).split(",")
                )
                .map(String::trim)
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .filter(value -> !value.isBlank())
                .toList();
    }

    // 기업 상세 : 진행 중인 채용공고
    public List<JobDTO> getCompanyJobPostingList(Long companyId) {
        return companyDao.selectCompanyJobPostingList(companyId);
    }

    // 기업 상세 : 채용공고 탭
    public List<JobDTO> getCompanyRecruitList(Long companyId) {

        List<JobDTO> list =
                companyDao.selectCompanyRecruitList(companyId);

        // 채용공고 스킬
        for (JobDTO job : list) {

            job.setSkillList(
                    companyDao.selectSkillListByJobPostingId(job.getJobPostingId())
            );
        }

        return list;
    }

    // 관리자 대시보드 : 승인 대기 기업 수
    public int getPendingApprovalCount() {
        return companyDao.selectPendingApprovalCount();
    }

    // 관리자 대시보드 : 심사 요청 후 days일 이상 처리되지 않은 기업 수
    public int getLongPendingApprovalCount(int days) {
        return companyDao.selectLongPendingApprovalCount(days);
    }

    // 관리자 대시보드 : 최근 기업 심사 요청 (최대 limit건)
    public List<CompanyDTO> getRecentApprovalRequests(int limit) {
        return companyDao.selectRecentApprovalRequests(limit);
    }

    // 관리자 대시보드 : 오늘 신규 기업 신청 수
    public int getTodayApplicationCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return companyDao.selectTodayApplicationCount(start, end);
    }

    // 기업 정보 : 관심기업 등록 및 해제
    @Transactional
    public boolean toggleWish(Long userId, Long companyId) {
        getCompanyDetail(companyId);

        int count = companyDao.countWish(userId, companyId);
        // 이미 관심기업이면 삭제
        if (count > 0) {
            companyDao.deleteWish(userId, companyId);
            return false;
        }
        // 관심기업이 아니면 등록

        companyDao.insertWish(userId, companyId);
        return true;
    }

    public boolean isWished(Long userId, Long companyId) {
        return userId != null && companyDao.countWish(userId, companyId) > 0;
    }

}