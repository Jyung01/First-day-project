package kr.co.firstdayproject.dao.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportDao {
    int selectTargetCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);
    int selectDuplicateCount(@Param("userId") Long userId, @Param("targetType") String targetType,
                             @Param("targetId") Long targetId);
    int insertReport(@Param("userId") Long userId, @Param("targetType") String targetType,
                     @Param("targetId") Long targetId, @Param("reasonCode") String reasonCode,
                     @Param("detail") String detail);
}
