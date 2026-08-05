package kr.co.firstdayproject.dao.company;

import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyDao {

    // 메인 : 인기 기업 조회
    List<CompanyDTO> selectPopularCompanyList();

    // 기업정보 :  기업 목록 조회
    List<CompanyDTO> selectCompanyList(CompanySearchDTO search);

    // 기업정보 :  기업 전체 개수
    int selectCompanyCount(CompanySearchDTO search);

    List<String> selectIndustryList();

    List<String> selectRegionList();

    List<String> selectCompanySizeList();

    List<JobCategoryOption> selectJobCategoryList();

    // 기업 상세
    CompanyDTO selectCompanyDetail(Long companyId);
}
