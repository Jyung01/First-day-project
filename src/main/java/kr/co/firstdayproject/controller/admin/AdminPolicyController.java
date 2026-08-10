package kr.co.firstdayproject.controller.admin.policy;

import kr.co.firstdayproject.dto.policy.PolicyDto;
import kr.co.firstdayproject.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/admin/cs/policy")
public class AdminPolicyController {

    private final PolicyService policyService;

    /** 약관 관리 목록 화면 */
    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("policies", policyService.getActivePolicies());
        return "admin/policy/policy";
    }

    /** 약관 상세 조회 (수정 모달을 열 때 AJAX로 호출) */
    @GetMapping("/{policyCode}")
    @ResponseBody
    public PolicyDto detail(@PathVariable String policyCode) {
        return policyService.getPolicyByCode(policyCode);
    }

    /** 약관 제목/본문 수정 */
    @PutMapping("/{policyCode}")
    @ResponseBody
    public ResponseEntity<Void> update(
            @PathVariable String policyCode,
            @RequestBody Map<String, String> body
    ) {
        // TODO: 관리자 로그인/세션 연동 후 실제 로그인한 관리자 ID로 교체
        Long adminId = 1L;
        policyService.updatePolicy(policyCode, body.get("title"), body.get("content"), adminId);
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