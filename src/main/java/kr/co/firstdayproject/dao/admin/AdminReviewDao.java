package kr.co.firstdayproject.dao.admin;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.admin.AdminReviewDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminReviewDao {
    List<AdminReviewDTO> selectAdminReviewList(Map<String, Object> search);
    int selectAdminReviewCount(Map<String, Object> search);
    Map<String, Object> selectAdminReviewSummary();
    AdminReviewDTO selectCompanyReviewDetail(Long reviewId);
    AdminReviewDTO selectInterviewReviewDetail(Long reviewId);
    int updateCompanyReviewStatus(@Param("reviewId") Long reviewId, @Param("status") String status,
                                  @Param("hiddenReason") String hiddenReason, @Param("adminId") Long adminId,
                                  @Param("memo") String memo);
    int updateInterviewReviewStatus(@Param("reviewId") Long reviewId, @Param("status") String status,
                                    @Param("hiddenReason") String hiddenReason, @Param("adminId") Long adminId,
                                    @Param("memo") String memo);
    int resolveReviewReports(@Param("reviewType") String reviewType, @Param("reviewId") Long reviewId,
                             @Param("adminId") Long adminId, @Param("memo") String memo);
}
