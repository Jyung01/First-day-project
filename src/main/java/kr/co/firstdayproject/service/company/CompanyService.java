package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyDao;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
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
}
