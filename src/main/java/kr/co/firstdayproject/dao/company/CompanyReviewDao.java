package kr.co.firstdayproject.dao.company;

import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyReviewDao {

    // 기업리뷰 등록
    int insertCompanyReview(CompanyReviewsDTO dto);

    // 기업 상세 - 기업리뷰 : 리뷰요약
    CompanyReviewsDTO selectCompanyReviewSummary(Long companyId);

    // 기업 상세 - 기업리뷰 : 리뷰목록
    List<CompanyReviewsDTO> selectCompanyReviewList(@Param("companyId") Long companyId,
                                                    @Param("offset") int offset,
                                                    @Param("pageSize") int pageSize,
                                                    @Param("sort") String sort);

    // 기업 상세 - 기업리뷰 : 리뷰 개수
    int selectCompanyReviewCount(Long companyId);

    // 기업 상세 - 면접후기 : 후기요약
    InterviewReviewsDTO selectInterviewReviewSummary(Long companyId);

    // 기업 상세 - 면접후기 : 후기목록
    List<InterviewReviewsDTO> selectInterviewReviewList(
            @Param("companyId") Long companyId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("sort") String sort
    );

    // 기업 상세 - 면접후기 : 후기 개수
    int selectInterviewReviewCount(Long companyId);
}
