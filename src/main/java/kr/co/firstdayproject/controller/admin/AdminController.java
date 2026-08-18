package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.cs.FaqService;
import kr.co.firstdayproject.service.cs.NoticeService;
import kr.co.firstdayproject.service.cs.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AwsS3Service s3UploadService;
    private final BannerService bannerService;
    private final QnaService qnaService;
    private final NoticeService noticeService;
    private final FaqService faqService;

    @GetMapping({"", "/index"})
    public String index(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }

    // salary 메서드 삭제됨 -> AdminSalaryController 가 전담
    // notice 메서드 삭제됨 -> AdminNoticeController 가 전담

    @GetMapping("/cs/faq")
    public String faq(Model model) {
        model.addAttribute("activeMenu", "cs");
        // 상단 통계 카드(미답변 문의/공지사항/FAQ)는 3개 화면(공지사항·FAQ·1:1 문의)에서
        // 공통으로 노출되므로 항상 세 값을 다 함께 담아 넘긴다.
        model.addAttribute("pendingCount", qnaService.getPendingCount());
        model.addAttribute("noticeCount", noticeService.getTotalCount());
        model.addAttribute("faqCount", faqService.getTotalCount());
        return "admin/cs/faq";
    }

    @GetMapping("/cs/qna")
    public String qna(Model model) {
        model.addAttribute("activeMenu", "cs");
        return "admin/cs/qna";
    }
}
