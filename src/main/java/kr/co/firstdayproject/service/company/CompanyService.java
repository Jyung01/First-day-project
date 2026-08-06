package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyDao;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.dto.job.JobDTO;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (company.getBenefits() != null) {
            company.setBenefitList(
                    Arrays.stream(company.getBenefits().split(","))
                            .map(String::trim)
                            .toList()
            );
        }

        return company;
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


}
