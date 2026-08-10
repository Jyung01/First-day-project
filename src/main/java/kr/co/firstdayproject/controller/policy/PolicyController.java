package kr.co.firstdayproject.controller.policy;

import kr.co.firstdayproject.dto.policy.PolicyDto;
import kr.co.firstdayproject.service.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 사용자 - 약관/정책 조회 화면
 * 좌측 목록(7개)에서 아무 항목이나 클릭하면 같은 화면에서 policyCode만 바뀌어 내용이 전환됩니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/policy")
public class PolicyController {

    private final PolicyService policyService;

    // 기존 화면에 남아있는 하드코딩 링크(@{/policy/terms}, @{/policy/privacy}) 호환용 별칭
    private static final String DEFAULT_TERMS_CODE = "SERVICE_TERMS_PERSONAL";
    private static final String DEFAULT_PRIVACY_CODE = "PUBLIC_TERMS";

    @GetMapping("/terms")
    public String terms(Model model) {
        return renderPolicy(DEFAULT_TERMS_CODE, model, "policy/terms");
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        return renderPolicy(DEFAULT_PRIVACY_CODE, model, "policy/privacy");
    }

    /** 공통 진입점: /policy/{policyCode} (예: /policy/MARKETING_OPTIONAL) — terms.html 템플릿을 재사용 */
    @GetMapping("/{policyCode}")
    public String view(@PathVariable String policyCode, Model model) {
        return renderPolicy(policyCode, model, "policy/terms");
    }

    private String renderPolicy(String policyCode, Model model, String viewName) {
        List<PolicyDto> policies = policyService.getActivePolicies();
        PolicyDto current = policyService.getActivePolicyByCode(policyCode);

        model.addAttribute("policies", policies);
        model.addAttribute("current", current);

        return viewName;
    }
}