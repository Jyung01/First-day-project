package kr.co.firstdayproject.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.co.firstdayproject.dto.auth.CorporateSignupRequest;
import kr.co.firstdayproject.dto.auth.CorporateTermsAgreement;
import kr.co.firstdayproject.dto.auth.PersonalSignupRequest;
import kr.co.firstdayproject.dto.auth.PersonalTermsAgreement;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.auth.*;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String PERSONAL_TERMS_SESSION_KEY = "personalTermsAgreement";
    private static final String CORPORATE_TERMS_SESSION_KEY = "corporateTermsAgreement";

    private final AuthService authService;
    private final JobService jobService;
    private final PersonalSignupService personalSignupService;
    private final CorporateSignupService corporateSignupService;

    /**
     * 이미 로그인한 사용자에게는 로그인 폼 대신 각자의 첫 화면을 보여준다.
     * 이동 경로는 로그인 성공 후 이동 규칙(LoginSuccessHandler)과 맞춘다.
     */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "auth/login";
        }
        return "redirect:" + resolveLandingPage(userDetails.getUserType());
    }

    private String resolveLandingPage(String userType) {
        return switch (userType) {
            case "관리자" -> "/admin";
            case "기업" -> "/corp";
            default -> "/";
        };
    }

    @GetMapping("/member-type")
    public String memberType() { return "auth/member-type"; }

    @GetMapping("/personal-terms")
    public String personalTerms(Model model, HttpSession session) {
        session.removeAttribute(PERSONAL_TERMS_SESSION_KEY);
        model.addAttribute(
                "policies",
                authService.getPersonalSignupPolicies()
        );
        return "auth/personal-terms";
    }

    @PostMapping("/personal-terms")
    public String acceptPersonalTerms(
            @RequestParam(name = "agreedPolicyIds", required = false)
            Set<Long> agreedPolicyIds,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        session.removeAttribute(PERSONAL_TERMS_SESSION_KEY);

        Set<Long> submittedPolicyIds = agreedPolicyIds == null
                ? Set.of()
                : new HashSet<>(agreedPolicyIds);

        Optional<PersonalTermsAgreement> agreement =
                authService.validatePersonalTermsAgreement(submittedPolicyIds);

        if (agreement.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "termsError",
                    "필수 약관에 모두 동의해주세요."
            );
            redirectAttributes.addFlashAttribute(
                    "submittedPolicyIds",
                    submittedPolicyIds
            );
            return "redirect:/auth/personal-terms";
        }

        session.setAttribute(
                PERSONAL_TERMS_SESSION_KEY,
                agreement.get()
        );
        return "redirect:/auth/personal-signup";
    }

    @GetMapping("/personal-signup")
    public String personalSignup(HttpSession session, Model model) {
        if (!(session.getAttribute(PERSONAL_TERMS_SESSION_KEY)
                instanceof PersonalTermsAgreement)) {
            return "redirect:/auth/personal-terms";
        }
        PersonalSignupRequest signupRequest = new PersonalSignupRequest();
        model.addAttribute("signupRequest", signupRequest);
        preparePersonalSignupModel(model, signupRequest, session);
        return "auth/personal-signup";
    }

    @PostMapping("/member/form")
    public String signupPersonalMember(
            @Valid @ModelAttribute("signupRequest")
            PersonalSignupRequest signupRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Object termsState = session.getAttribute(PERSONAL_TERMS_SESSION_KEY);
        if (!(termsState instanceof PersonalTermsAgreement termsAgreement)) {
            redirectAttributes.addFlashAttribute(
                    "termsError",
                    "회원가입을 계속하려면 약관에 다시 동의해주세요."
            );
            return "redirect:/auth/personal-terms";
        }

        VerifiedEmail verifiedEmail = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        ) instanceof VerifiedEmail state ? state : null;

        if (bindingResult.hasErrors()) {
            preparePersonalSignupModel(model, signupRequest, session);
            return "auth/personal-signup";
        }

        try {
            personalSignupService.signup(
                    signupRequest,
                    termsAgreement,
                    verifiedEmail,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
        } catch (PersonalSignupException exception) {
            if ("terms".equals(exception.getField())) {
                session.removeAttribute(PERSONAL_TERMS_SESSION_KEY);
                redirectAttributes.addFlashAttribute(
                        "termsError",
                        exception.getMessage()
                );
                return "redirect:/auth/personal-terms";
            }

            if (exception.getField() == null) {
                bindingResult.reject("signup.failed", exception.getMessage());
            } else {
                bindingResult.rejectValue(
                        exception.getField(),
                        "signup.invalid",
                        exception.getMessage()
                );
            }
            preparePersonalSignupModel(model, signupRequest, session);
            return "auth/personal-signup";
        }

        session.removeAttribute(PERSONAL_TERMS_SESSION_KEY);
        session.removeAttribute(EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY);
        return "redirect:/auth/personal-complete";
    }

    private void preparePersonalSignupModel(
            Model model,
            PersonalSignupRequest signupRequest,
            HttpSession session
    ) {
        List<JobCategoryGroup> groups = jobService.getActiveJobCategoryGroups();
        model.addAttribute("jobCategoryGroups", groups);

        List<Long> selectedIds = signupRequest.getDesiredJobIds() == null
                ? List.of()
                : signupRequest.getDesiredJobIds();
        Map<Long, JobCategoryOption> optionsById = new LinkedHashMap<>();
        groups.forEach(group -> group.children().forEach(
                option -> optionsById.put(option.jobCategoryId(), option)
        ));
        List<JobCategoryOption> selectedJobs = new ArrayList<>();
        selectedIds.forEach(id -> {
            JobCategoryOption option = optionsById.get(id);
            if (option != null && selectedJobs.stream().noneMatch(
                    selected -> selected.jobCategoryId().equals(id)
            )) {
                selectedJobs.add(option);
            }
        });
        model.addAttribute("selectedJobs", selectedJobs);

        model.addAttribute(
                "loginIdAvailable",
                personalSignupService.isLoginIdAvailable(signupRequest.getMemberId())
        );
        VerifiedEmail verifiedEmail = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        ) instanceof VerifiedEmail state ? state : null;
        model.addAttribute(
                "emailVerified",
                personalSignupService.isVerifiedEmail(
                        signupRequest.getEmail(),
                        verifiedEmail
                )
        );
    }

    @GetMapping("/personal-complete")
    public String personalComplete() { return "auth/personal-complete"; }

    @GetMapping("/corporate-terms")
    public String corporateTerms(Model model, HttpSession session) {
        session.removeAttribute(CORPORATE_TERMS_SESSION_KEY);
        model.addAttribute(
                "policies",
                authService.getCorporateSignupPolicies()
        );
        return "auth/corporate-terms";
    }

    @PostMapping("/corporate-terms")
    public String acceptCorporateTerms(
            @RequestParam(name = "agreedPolicyIds", required = false)
            Set<Long> agreedPolicyIds,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        session.removeAttribute(CORPORATE_TERMS_SESSION_KEY);

        Set<Long> submittedPolicyIds = agreedPolicyIds == null
                ? Set.of()
                : new HashSet<>(agreedPolicyIds);

        Optional<CorporateTermsAgreement> agreement =
                authService.validateCorporateTermsAgreement(submittedPolicyIds);

        if (agreement.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "termsError",
                    "필수 약관에 모두 동의해주세요."
            );
            redirectAttributes.addFlashAttribute(
                    "submittedPolicyIds",
                    submittedPolicyIds
            );
            return "redirect:/auth/corporate-terms";
        }

        session.setAttribute(
                CORPORATE_TERMS_SESSION_KEY,
                agreement.get()
        );
        return "redirect:/auth/corporate-signup";
    }

    @GetMapping("/corporate-signup")
    public String corporateSignup(HttpSession session, Model model) {
        if (!(session.getAttribute(CORPORATE_TERMS_SESSION_KEY)
                instanceof CorporateTermsAgreement)) {
            return "redirect:/auth/corporate-terms";
        }
        CorporateSignupRequest signupRequest = new CorporateSignupRequest();
        model.addAttribute("signupRequest", signupRequest);
        prepareCorporateSignupModel(model, signupRequest, session);
        return "auth/corporate-signup";
    }

    @PostMapping("/company/form")
    public String signupCorporateMember(
            @Valid @ModelAttribute("signupRequest")
            CorporateSignupRequest signupRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Object termsState = session.getAttribute(CORPORATE_TERMS_SESSION_KEY);
        if (!(termsState instanceof CorporateTermsAgreement termsAgreement)) {
            redirectAttributes.addFlashAttribute(
                    "termsError",
                    "회원가입을 계속하려면 약관에 다시 동의해주세요."
            );
            return "redirect:/auth/corporate-terms";
        }

        VerifiedEmail verifiedEmail = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        ) instanceof VerifiedEmail state ? state : null;

        if (bindingResult.hasErrors()) {
            prepareCorporateSignupModel(model, signupRequest, session);
            return "auth/corporate-signup";
        }

        try {
            corporateSignupService.signup(
                    signupRequest,
                    termsAgreement,
                    verifiedEmail,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
        } catch (CorporateSignupException exception) {
            if ("terms".equals(exception.getField())) {
                session.removeAttribute(CORPORATE_TERMS_SESSION_KEY);
                redirectAttributes.addFlashAttribute(
                        "termsError",
                        exception.getMessage()
                );
                return "redirect:/auth/corporate-terms";
            }

            if (exception.getField() == null) {
                bindingResult.reject("signup.failed", exception.getMessage());
            } else {
                bindingResult.rejectValue(
                        exception.getField(),
                        "signup.invalid",
                        exception.getMessage()
                );
            }
            prepareCorporateSignupModel(model, signupRequest, session);
            return "auth/corporate-signup";
        }

        session.removeAttribute(CORPORATE_TERMS_SESSION_KEY);
        session.removeAttribute(EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY);
        return "redirect:/auth/corporate-complete";
    }

    private void prepareCorporateSignupModel(
            Model model,
            CorporateSignupRequest signupRequest,
            HttpSession session
    ) {
        model.addAttribute(
                "loginIdAvailable",
                corporateSignupService.isLoginIdAvailable(signupRequest.getMemberId())
        );
        model.addAttribute(
                "businessNumberAvailable",
                corporateSignupService.isBusinessNumberAvailable(
                        signupRequest.getBusinessNumber()
                )
        );
        VerifiedEmail verifiedEmail = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        ) instanceof VerifiedEmail state ? state : null;
        model.addAttribute(
                "emailVerified",
                corporateSignupService.isVerifiedEmail(
                        signupRequest.getManagerEmail(),
                        verifiedEmail
                )
        );
    }

    @GetMapping("/corporate-complete")
    public String corporateComplete() { return "auth/corporate-complete"; }

    @GetMapping("/find-id")
    public String findId() { return "auth/find-id"; }

    @GetMapping("/reset-password")
    public String resetPassword(HttpSession session) {
        session.removeAttribute(EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY);
        return "auth/reset-password";
    }
}
