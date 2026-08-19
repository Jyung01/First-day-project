package kr.co.firstdayproject.service.admin;

import kr.co.firstdayproject.dao.admin.AdminDao;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AdminDao adminDao;
    private final UserRepository userRepository;

    // 관리자 대시보드 : 미처리 신고 수
    public int getUnresolvedReportCount() {
        return adminDao.selectUnresolvedReportCount();
    }

    // 관리자 대시보드 : 오늘 접수된 신고 수
    public int getTodayReportCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return adminDao.selectTodayReportCount(start, end);
    }

    // 관리자 대시보드 : 오늘 신규 가입한 개인회원 수
    public long getTodayNewUserCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return userRepository.countByUserTypeAndCreatedAtBetween("개인", start, end);
    }

}