package kr.co.firstdayproject.dao.company;

import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.dto.job.JobDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyDao {

    // 메인 : 인기 기업 조회
    List<CompanyDTO> selectPopularCompanyList();

    // 기업정보 :  기업 목록 조회
    List<CompanyDTO> selectCompanyList(CompanySearchDTO search);

    // 기업정보 :  기업 개수, 산업, 지역, 기업규모, 직무 조회
    int selectCompanyCount(CompanySearchDTO search);
    List<String> selectIndustryList();
    List<String> selectRegionList();
    List<String> selectCompanySizeList();
    List<JobCategoryOption> selectJobCategoryList();

    // 기업 상세
    CompanyDTO selectCompanyDetail(Long companyId);

    // 기업 상세 - 기업상세 : 진행 중인 채용공고
    List<JobDTO> selectCompanyJobPostingList(Long companyId);

    // 기업 상세 - 채용공고 : 채용공고 조회
    List<JobDTO> selectCompanyRecruitList(Long companyId);

    // 기업 상세 - 채용공고 : 스킬 조회
    List<String> selectSkillListByJobPostingId(Long jobPostingId);

    // 관리자 대시보드 : 승인 대기 기업 수
    int selectPendingApprovalCount();
    // 기업 정보 - 관심기업 조회
    int countWish(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId
    );

    // 기업 정보 - 관심기업 등록
    int insertWish(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId
    );

    // 기업 정보 - 관심기업 삭제
    int deleteWish(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId
    );

    // 관리자 대시보드 : 최근 기업 심사 요청 목록
    List<CompanyDTO> selectRecentApprovalRequests(@Param("limit") int limit);

    // 관리자 대시보드 : 오늘 신규 기업 신청 수
    int selectTodayApplicationCount(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

}