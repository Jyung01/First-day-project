package kr.co.firstdayproject.controller.corp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kr.co.firstdayproject.config.CompanyReviewGateInterceptor;
import kr.co.firstdayproject.dto.corp.CompanyAccountActionResponse;
import kr.co.firstdayproject.dto.corp.CompanyAccountRequest;
import kr.co.firstdayproject.dto.corp.CompanyPasswordChangeRequest;
import kr.co.firstdayproject.dto.corp.CompanyProfileRequest;
import kr.co.firstdayproject.dto.corp.CompanyReapplyRequest;
import kr.co.firstdayproject.dto.corp.CompanyWithdrawalRequest;
import kr.co.firstdayproject.dto.corp.CompanyWithdrawalSummary;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.company.CompanyRejectionType;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.corp.CompanyProfileUpdateException;
import kr.co.firstdayproject.service.corp.CompanyReapplyException;
import kr.co.firstdayproject.service.corp.CompanyAccountException;
import kr.co.firstdayproject.service.corp.CorpService;
import kr.co.firstdayproject.service.corp.CorpDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/corp")
@RequiredArgsConstructor
public class CorpController {

    private static final String REJECTED_APPROVAL = "반려";
    private static final String DEFAULT_REJECTION_REASON =
            "반려 사유가 등록되지 않았습니다. 고객센터에 문의해주세요.";

    private final CorpService corpService;
    private final CorpDashboardService corpDashboardService;

    @GetMapping({"", "/index"})
    public String index(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }
        if (isRejectedCompany(userDetails)) {
            return "redirect:/corp/company-info-rejected";
        }

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("companyStatus", "APPROVED");
        model.addAttribute(
                "dashboard",
                corpDashboardService.getDashboard(userDetails.getCompanyId())
        );

        return "corp/index";
    }

    @GetMapping("/company-info")
    public String companyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp/company-info-rejected";
        }

        model.addAttribute("companyForm", CompanyProfileRequest.from(company));
        addCompanyProfileModel(model, company, userDetails);
        return "corp/company-info";
    }

    /**
     * 기업정보 저장. {@code requestReview=true}면 저장한 내용으로 곧바로 심사까지 요청한다.
     *
     * <p>심사 요청을 별도 요청으로 두면, 화면에서 값을 고치고 저장을 누르지 않은 채
     * 심사를 요청했을 때 <b>고친 내용이 버려지고 예전에 저장된 내용이 심사에 올라간다.</b>
     * 심사 요청 직후에는 로그인이 막혀 되돌릴 수도 없다.
     * 그래서 저장과 심사 요청을 한 요청에서 순서대로 처리한다.
     */
    @PostMapping("/company-info")
    public String updateCompanyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("companyForm") CompanyProfileRequest request,
            BindingResult bindingResult,
            @RequestParam(name = "companyLogo", required = false)
            MultipartFile companyLogo,
            @RequestParam(name = "requestReview", defaultValue = "false")
            boolean requestReview,
            HttpServletRequest httpRequest,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp/company-info-rejected";
        }

        if (bindingResult.hasErrors()) {
            addCompanyProfileModel(model, company, userDetails);
            return "corp/company-info";
        }

        try {
            corpService.updateCompanyProfile(
                    userDetails.getCompanyId(),
                    request,
                    companyLogo
            );
        } catch (CompanyProfileUpdateException exception) {
            bindingResult.reject("company.profile", exception.getMessage());
            addCompanyProfileModel(model, company, userDetails);
            return "corp/company-info";
        }

        if (!requestReview) {
            return "redirect:/corp/company-info?saved=true";
        }

        try {
            corpService.requestInitialReview(userDetails.getCompanyId());
        } catch (IllegalStateException exception) {
            // 저장은 이미 끝났으므로 되돌리지 않고, 심사 요청만 실패했음을 알린다.
            bindingResult.reject("company.reviewRequest", exception.getMessage());
            addCompanyProfileModel(
                    model,
                    corpService.getCompany(userDetails.getCompanyId()),
                    userDetails
            );
            return "corp/company-info";
        }

        /*
         * 심사 요청 직후부터 LoginSuccessHandler가 로그인을 막는다.
         * 세션을 남겨두면 인증된 채로 인터셉터에 계속 막히는 상태가 되므로 정리한다.
         */
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/auth/login?companyApproval=requested";
    }

    private void addCompanyProfileModel(
            Model model,
            Company company,
            CustomUserDetails userDetails
    ) {
        /*
         * 가입 후 아직 심사를 요청하지 않은 상태. 화면에서 안내 문구와
         * "심사 요청" 버튼을 노출할지, 사이드바 메뉴를 잠글지 판단하는 데 쓴다.
         */
        boolean draft = CompanyReviewGateInterceptor.isDraft(company);

        model.addAttribute("activeMenu", "company");
        model.addAttribute("companyStatus", draft ? "DRAFT" : "APPROVED");
        model.addAttribute("reviewDraft", draft);
        model.addAttribute("companyLogoUrl", company.getLogoUrl());
        model.addAttribute("managerName", userDetails.getDisplayName());
        model.addAttribute(
                "managerPhone",
                corpService.getManagerPhone(userDetails.getUserId())
        );
        model.addAttribute(
                "businessNumber",
                formatBusinessNumber(company.getBusinessNumber())
        );
        model.addAttribute(
                "approvalStatus",
                "승인".equals(company.getApprovalStatus())
                        ? "승인 완료"
                        : company.getApprovalStatus()
        );
    }

    private String formatBusinessNumber(String businessNumber) {
        if (businessNumber == null) {
            return "";
        }
        String digits = businessNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return businessNumber;
        }
        return digits.substring(0, 3)
                + "-" + digits.substring(3, 5)
                + "-" + digits.substring(5);
    }

    @GetMapping("/company-info-rejected")
    public String companyInfoRejected(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (!REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp";
        }

        model.addAttribute("companyForm", CompanyReapplyRequest.from(company));
        addRejectedCompanyModel(model, company, userDetails.getUserId());
        return "corp/company-info-rejected";
    }

    @PostMapping("/company-info-rejected")
    public String requestCompanyReapproval(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("companyForm") CompanyReapplyRequest request,
            BindingResult bindingResult,
            @RequestParam(name = "companyLogo", required = false)
            MultipartFile companyLogo,
            Model model,
            HttpServletRequest httpRequest
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (!REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp";
        }

        if (bindingResult.hasErrors()) {
            addRejectedCompanyModel(model, company, userDetails.getUserId());
            return "corp/company-info-rejected";
        }

        try {
            corpService.requestReapproval(
                    userDetails.getCompanyId(),
                    request,
                    companyLogo
            );
        } catch (CompanyReapplyException exception) {
            if ("companyLogo".equals(exception.getField())) {
                model.addAttribute("companyLogoError", exception.getMessage());
            } else if (exception.getField() == null) {
                bindingResult.reject("company.reapply", exception.getMessage());
            } else {
                bindingResult.rejectValue(
                        exception.getField(),
                        "company.reapply",
                        exception.getMessage()
                );
            }
            addRejectedCompanyModel(model, company, userDetails.getUserId());
            return "corp/company-info-rejected";
        }

        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/auth/login?companyApproval=reapplied";
    }

    private void addRejectedCompanyModel(
            Model model,
            Company company,
            Long userId
    ) {
        model.addAttribute("activeMenu", "company");
        model.addAttribute("companyStatus", "REJECTED");
        model.addAttribute("managerPhone", corpService.getManagerPhone(userId));
        model.addAttribute("companyLogoUrl", company.getLogoUrl());
        model.addAttribute("companyAddress", Stream.of(
                        company.getPostalCode(),
                        company.getAddressLine1(),
                        company.getAddressLine2()
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" ")));
        CompanyRejectionType rejectionType = CompanyRejectionType
                .fromCode(company.getLatestRejectionCode())
                .orElse(null);
        model.addAttribute(
                "rejectedFields",
                rejectionType == null
                        ? Set.of()
                        : rejectionType.getRejectedFieldCodes()
        );
        model.addAttribute(
                "rejectedFieldSummary",
                rejectionType == null
                        ? ""
                        : rejectionType.getRejectedFieldSummary()
        );
        model.addAttribute(
                "rejectionReason",
                company.getLatestRejectionReason() == null
                        || company.getLatestRejectionReason().isBlank()
                        ? rejectionType == null
                                ? DEFAULT_REJECTION_REASON
                                : rejectionType.getDefaultMessage()
                        : company.getLatestRejectionReason()
        );
    }

    @GetMapping("/account")
    public String account(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }
        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp/company-info-rejected";
        }
        User manager = corpService.getCompanyManager(userDetails.getUserId());
        CompanyWithdrawalSummary withdrawalSummary =
                corpService.getWithdrawalSummary(userDetails.getCompanyId());
        model.addAttribute("accountForm", CompanyAccountRequest.from(manager));
        model.addAttribute(
                "activeJobCount",
                withdrawalSummary.activeJobCount()
        );
        model.addAttribute(
                "activeApplicantCount",
                withdrawalSummary.activeApplicantCount()
        );
        addCompanyAccountModel(model, manager);
        return "corp/account";
    }

    @PostMapping("/account")
    public String updateAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("accountForm") CompanyAccountRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }
        Company company = corpService.getCompany(userDetails.getCompanyId());
        if (REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            return "redirect:/corp/company-info-rejected";
        }
        User manager = corpService.getCompanyManager(userDetails.getUserId());
        if (bindingResult.hasErrors()) {
            addCompanyAccountModel(model, manager);
            return "corp/account";
        }
        corpService.updateCompanyManager(userDetails.getUserId(), request);
        return "redirect:/corp/account?saved=true";
    }

    @PostMapping("/account/password")
    @ResponseBody
    public ResponseEntity<CompanyAccountActionResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CompanyPasswordChangeRequest request
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CompanyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            corpService.changePassword(userDetails.getUserId(), request);
            return ResponseEntity.ok(CompanyAccountActionResponse.success(
                    "비밀번호가 변경되었습니다."
            ));
        } catch (CompanyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    CompanyAccountActionResponse.failure(exception.getMessage())
            );
        }
    }

    @PostMapping("/account/withdraw")
    @ResponseBody
    public ResponseEntity<CompanyAccountActionResponse> withdrawAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CompanyWithdrawalRequest request,
            HttpServletRequest httpRequest
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CompanyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            corpService.withdrawCompany(
                    userDetails.getUserId(),
                    userDetails.getCompanyId(),
                    request.currentPassword()
            );
        } catch (CompanyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    CompanyAccountActionResponse.failure(exception.getMessage())
            );
        }

        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(CompanyAccountActionResponse.success(
                "기업회원 탈퇴가 완료되었습니다.",
                "/auth/login?accountStatus=withdrawn"
        ));
    }

    private void addCompanyAccountModel(Model model, User manager) {
        model.addAttribute("activeMenu", "account");
        model.addAttribute("companyStatus", "APPROVED");
        model.addAttribute("loginId", manager.getLoginId());
        model.addAttribute("managerName", manager.getName());
        model.addAttribute("managerEmail", manager.getEmail());
        model.addAttribute("managerPhone", manager.getPhone());
        model.addAttribute(
                "passwordChangedAt",
                manager.getPasswordChangedAt() == null
                        ? "-"
                        : manager.getPasswordChangedAt().format(
                                DateTimeFormatter.ofPattern("yyyy.MM.dd")
                        )
        );
    }

    private boolean isRejectedCompany(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return false;
        }

        Company company = corpService.getCompany(userDetails.getCompanyId());
        return REJECTED_APPROVAL.equals(company.getApprovalStatus());
    }
}
