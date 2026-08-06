package kr.co.firstdayproject.dao.company;

import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyReviewDao {

    // 기업 상세 - 기업리뷰 : 리뷰요약
    CompanyReviewsDTO selectCompanyReviewSummary(Long companyId);

    // 기업 상세 - 기업리뷰 : 리뷰목록
    List<CompanyReviewsDTO> selectCompanyReviewList( @Param("companyId") Long companyId,
                                                     @Param("offset") int offset,
                                                     @Param("pageSize") int pageSize);
    // 기업 상세 - 기업리뷰 : 리뷰 개수
    int selectCompanyReviewCount(Long companyId);
}
