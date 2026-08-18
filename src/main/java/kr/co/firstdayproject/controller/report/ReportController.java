package kr.co.firstdayproject.controller.report;

import java.util.Map;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> report(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam String reportType, @RequestParam Long targetId,
                                    @RequestParam String reasonCode, @RequestParam(required = false) String detail) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        try {
            reportService.report(userDetails.getUserId(), reportType, targetId, reasonCode, detail);
            return ResponseEntity.ok(Map.of("success", true, "message", "신고가 접수되었습니다."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
