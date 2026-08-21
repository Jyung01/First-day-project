package kr.co.firstdayproject.controller.admin;

import java.util.Map;
import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminSalaryService;
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
@RequestMapping("/admin/salary")
public class AdminSalaryController {
    private final AdminSalaryService adminSalaryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "all") String status,
                       @RequestParam(required = false) Long companyId,
                       @RequestParam(required = false) Long jobCategoryId,
                       @RequestParam(defaultValue = "") String career,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "latest") String sort,
                       Model model) {
        model.addAllAttributes(adminSalaryService.getList(page, status, companyId,
                jobCategoryId, career, keyword, sort));
        model.addAttribute("activeMenu", "salary");
        return "admin/salary/index";
    }

    @GetMapping("/detail")
    @ResponseBody
    public ResponseEntity<?> detail(@RequestParam Long salaryRecordId) {
        try {
            SalaryRecordsDTO detail = adminSalaryService.getDetail(salaryRecordId);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/review")
    @ResponseBody
    public ResponseEntity<?> review(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam Long salaryRecordId,
                                    @RequestParam String status,
                                    @RequestParam(required = false) String hiddenReason) {
        try {
            Long adminId = AdminPrincipal.requireAdminId(userDetails);
            adminSalaryService.review(adminId, salaryRecordId, status, hiddenReason);
            return ResponseEntity.ok(Map.of("success", true, "message", "연봉정보 검토 결과가 저장되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
