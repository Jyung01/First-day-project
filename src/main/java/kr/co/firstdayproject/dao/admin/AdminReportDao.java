package kr.co.firstdayproject.dao.admin;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.admin.AdminReportDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminReportDao {
    List<AdminReportDTO> selectReportList(Map<String, Object> search);
    int selectReportCount(Map<String, Object> search);
    Map<String, Object> selectReportSummary(Map<String, Object> search);
    AdminReportDTO selectReportDetail(Long reportId);
    int rejectReport(@Param("reportId") Long reportId, @Param("adminId") Long adminId, @Param("memo") String memo);
    int resolveReportsForTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                                @Param("adminId") Long adminId, @Param("memo") String memo);
    int hideCompany(@Param("targetId") Long targetId);
    int hideJobPosting(@Param("targetId") Long targetId, @Param("adminId") Long adminId, @Param("memo") String memo);
    int hideCompanyReview(@Param("targetId") Long targetId, @Param("adminId") Long adminId, @Param("memo") String memo);
    int hideInterviewReview(@Param("targetId") Long targetId, @Param("adminId") Long adminId, @Param("memo") String memo);
}
