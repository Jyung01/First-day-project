package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.policy.PolicyDto;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 관리자 - 약관/정책 관리
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/config/policy")
public class AdminPolicyController {

    private final PolicyService policyService;

    /** 약관 관리 목록 화면 */
    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("activeMenu", "policy");
        model.addAttribute("policies", policyService.getActivePolicies());
        return "admin/config/policy";
    }

    /** 약관 상세 조회 (수정 모달을 열 때 AJAX로 호출) */
    @GetMapping("/{policyCode}")
    @ResponseBody
    public PolicyDto detail(@PathVariable String policyCode) {
        return policyService.getPolicyByCode(policyCode);
    }

    /** 약관 제목/본문/구분(필수·선택) 수정 */
    @PutMapping("/{policyCode}")
    @ResponseBody
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String policyCode,
            @RequestBody Map<String, String> body
    ) {
        Long adminId = AdminPrincipal.requireAdminId(userDetails);
        // "consentType"은 공개 정책(PUBLIC) 행에서는 전송되지 않으므로 null일 수 있음 -> 기존 값 유지
        policyService.updatePolicy(policyCode, body.get("title"), body.get("content"), body.get("consentType"), adminId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(404).body(Map.of("message", ex.getMessage()));
    }
}