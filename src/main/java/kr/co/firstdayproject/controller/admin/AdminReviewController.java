package kr.co.firstdayproject.controller.admin;

import java.util.Map;
import kr.co.firstdayproject.dto.admin.AdminReviewDTO;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/review")
public class AdminReviewController {
    private final AdminReviewService adminReviewService;

    @GetMapping
    public String list(@RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="company") String tab,
                       @RequestParam(defaultValue="") String keyword,
                       @RequestParam(defaultValue="latest") String sort, Model model) {
        model.addAllAttributes(adminReviewService.getList(page, tab, keyword, sort));
        model.addAttribute("activeMenu", "review");
        return "admin/review/index";
    }

    @GetMapping("/detail") @ResponseBody
    public ResponseEntity<?> detail(@RequestParam String reviewType, @RequestParam Long reviewId) {
        try {
            AdminReviewDTO detail = adminReviewService.getDetail(reviewType, reviewId);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/status") @ResponseBody
    public ResponseEntity<?> updateStatus(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestParam String reviewType, @RequestParam Long reviewId,
                                          @RequestParam String status,
                                          @RequestParam(required=false) String hiddenReason,
                                          @RequestParam(required=false) String memo) {
        try {
            Long adminId = AdminPrincipal.requireAdminId(userDetails);
            adminReviewService.updateStatus(adminId, reviewType, reviewId, status, hiddenReason, memo);
            return ResponseEntity.ok(Map.of("success", true, "message", "후기 상태가 변경되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
