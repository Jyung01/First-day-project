package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.dto.company.CompanyDTO;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import kr.co.firstdayproject.service.admin.AdminJobService;
import kr.co.firstdayproject.service.admin.AdminService;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyReviewService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.cs.FaqService;
import kr.co.firstdayproject.service.cs.NoticeService;
import kr.co.firstdayproject.service.cs.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    /** 이 일수 이상 심사가 처리되지 않은 기업을 "지연"으로 본다. */
    private static final int LONG_PENDING_DAYS = 3;

    private final AwsS3Service s3UploadService;
    private final BannerService bannerService;
    private final QnaService qnaService;
    private final NoticeService noticeService;
    private final FaqService faqService;
    private final AdminJobService adminJobService;
    private final AdminService adminService;
    private final CompanyService companyService;
    private final CompanyReviewService companyReviewService;

    @GetMapping({"", "/index"})
    public String index(Model model) {
        model.addAttribute("activeMenu", "dashboard");

        Map<String, Long> jobStats = adminJobService.getStatistics();
        Long todayJobPostCount = jobStats.get("todayCreated");

        // 상단 통계 카드
        model.addAttribute("pendingCompanyCount", companyService.getPendingApprovalCount());
        model.addAttribute("todayJobPostCount", todayJobPostCount);
        model.addAttribute("unresolvedReportCount", adminService.getUnresolvedReportCount());
        model.addAttribute("pendingQnaCount", qnaService.getPendingCount());

        /*
         * 카드 아래 보조 문구. 총량만으로는 "밀려 있는지"를 알 수 없어
         * 지연된 건수와 전일 대비 증감을 함께 보여준다.
         */
        model.addAttribute(
                "longPendingCompanyCount",
                companyService.getLongPendingApprovalCount(LONG_PENDING_DAYS)
        );
        model.addAttribute("longPendingDays", LONG_PENDING_DAYS);
        model.addAttribute(
                "jobPostCountChange",
                todayJobPostCount - jobStats.get("yesterdayCreated")
        );
        model.addAttribute("pendingQnaOverDayCount", qnaService.getPendingOverDayCount());

        // 최근 기업 심사 요청 (최대 5건)
        List<CompanyDTO> recentCompanyReviews = companyService.getRecentApprovalRequests(5);
        model.addAttribute("recentCompanyReviews", recentCompanyReviews);

        // 오늘의 운영 현황
        model.addAttribute("todayNewUserCount", adminService.getTodayNewUserCount());
        model.addAttribute("todayApplicationCount", companyService.getTodayApplicationCount());
        model.addAttribute("todayJobPostCreatedCount", todayJobPostCount);
        model.addAttribute("todayReviewCount", companyReviewService.getTodayReviewCount());
        model.addAttribute("todayReportCount", adminService.getTodayReportCount());
        model.addAttribute("todayAnsweredQnaCount", qnaService.getTodayAnsweredCount());

        return "admin/index";
    }

    // salary 메서드 삭제됨 -> AdminSalaryController 가 전담
    // notice 메서드 삭제됨 -> AdminNoticeController 가 전담

    @GetMapping("/cs/faq")
    public String faq(Model model) {
        model.addAttribute("activeMenu", "cs");
        model.addAttribute("pendingCount", qnaService.getPendingCount());
        model.addAttribute("noticeCount", noticeService.getTotalCount());
        model.addAttribute("faqCount", faqService.getTotalCount());
        return "admin/cs/faq";
    }

}
