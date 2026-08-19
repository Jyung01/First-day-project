package kr.co.firstdayproject.dao.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminDao {

    // 관리자 대시보드 : 미처리 신고 수 (status = '미처리')
    int selectUnresolvedReportCount();

    // 관리자 대시보드 : 오늘 접수된 신고 수
    int selectTodayReportCount(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

}