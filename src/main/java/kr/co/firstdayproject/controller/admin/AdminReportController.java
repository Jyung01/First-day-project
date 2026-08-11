package kr.co.firstdayproject.controller.admin;

import java.util.Map;
import kr.co.firstdayproject.dto.admin.AdminReportDTO;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/report")
public class AdminReportController {
    private final AdminReportService adminReportService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "all") String tab,
                       @RequestParam(defaultValue = "") String targetType, @RequestParam(defaultValue = "") String reasonCode,
                       @RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAllAttributes(adminReportService.getList(page, tab, targetType, reasonCode, keyword));
        model.addAttribute("activeMenu", "report");
        return "admin/report/index";
    }

    @GetMapping("/detail") @ResponseBody
    public ResponseEntity<?> detail(@RequestParam Long reportId) {
        try { return ResponseEntity.ok(adminReportService.getDetail(reportId)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PostMapping("/process") @ResponseBody
    public ResponseEntity<?> process(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long reportId,
                                     @RequestParam String action, @RequestParam(required = false) String memo) {
        try {
            adminReportService.process(userDetails == null ? 1L : userDetails.getUserId(), reportId, action, memo);
            return ResponseEntity.ok(Map.of("success", true, "message", "신고 처리가 완료되었습니다."));
        } catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }
}
