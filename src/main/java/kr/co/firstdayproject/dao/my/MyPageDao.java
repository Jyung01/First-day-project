package kr.co.firstdayproject.dao.my;

import java.util.List;
import kr.co.firstdayproject.dto.job.JobDTO;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MyPageDao {
    List<JobDTO> selectSavedJobList(@Param("userId") Long userId,
                                    @Param("filter") String filter,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    int selectSavedJobCount(@Param("userId") Long userId,
                            @Param("filter") String filter);

    int deleteSavedJob(@Param("userId") Long userId,
                       @Param("jobPostingId") Long jobPostingId);

    List<CompanyDTO> selectSavedCompanyList(@Param("userId") Long userId,
                                            @Param("sort") String sort,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    int selectSavedCompanyCount(@Param("userId") Long userId);

    int deleteSavedCompany(@Param("userId") Long userId,
                           @Param("companyId") Long companyId);

    List<CompanyReviewsDTO> selectMyCompanyReviews(@Param("userId") Long userId);
    List<InterviewReviewsDTO> selectMyInterviewReviews(@Param("userId") Long userId);
    int selectMyCompanyReviewCount(@Param("userId") Long userId);
    int selectMyInterviewReviewCount(@Param("userId") Long userId);
    CompanyReviewsDTO selectMyCompanyReview(@Param("userId") Long userId,
                                            @Param("reviewId") Long reviewId);
    InterviewReviewsDTO selectMyInterviewReview(@Param("userId") Long userId,
                                                @Param("reviewId") Long reviewId);
    int updateMyCompanyReview(CompanyReviewsDTO dto);
    int updateMyInterviewReview(InterviewReviewsDTO dto);
    int deleteMyCompanyReview(@Param("userId") Long userId,
                              @Param("reviewId") Long reviewId);
    int deleteMyInterviewReview(@Param("userId") Long userId,
                                @Param("reviewId") Long reviewId);
}
