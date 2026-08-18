package kr.co.firstdayproject.controller.my;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.firstdayproject.dto.my.MyAccountActionResponse;
import kr.co.firstdayproject.dto.my.MyPasswordChangeRequest;
import kr.co.firstdayproject.dto.my.MyWithdrawalRequest;
import kr.co.firstdayproject.dto.my.ProfileEditRequest;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.job.JobService;
import kr.co.firstdayproject.service.my.MyAccountException;
import kr.co.firstdayproject.service.my.MyApplicationQueryService;
import kr.co.firstdayproject.service.my.MyApplicationDetailService;
import kr.co.firstdayproject.service.my.MyPageException;
import kr.co.firstdayproject.service.my.MyPageService;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyPageController {

    private final JobService jobService;
    private final MyPageService myPageService;
    private final MyApplicationQueryService myApplicationQueryService;
    private final MyApplicationDetailService myApplicationDetailService;

    @GetMapping({"", "/index"})
    public String index(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        Long userId = userDetails.getUserId();
        model.addAttribute("activeMenu", "home");
        model.addAttribute("dashboardStats", myPageService.getDashboardStats(userId));
        model.addAttribute("recentApplications", myPageService.getRecentApplications(userId));
        myPageService.getRecentResume(userId)
                .ifPresent(resume -> model.addAttribute("recentResume", resume));
        myPageService.getRecentCoverLetter(userId)
                .ifPresent(coverLetter -> model.addAttribute("recentCoverLetter", coverLetter));
        return "my/index";
    }

    @GetMapping("/profile-edit")
    public String profileEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = myPageService.getUser(userDetails.getUserId());
        PersonalProfile profile = resolveProfile(model, userDetails.getUserId());

        model.addAttribute("profileForm", ProfileEditRequest.from(user, profile));
        addProfileEditModel(model, userDetails.getUserId(), user, profile);
        return "my/profile-edit";
    }

    @PostMapping("/profile-edit")
    public String updateProfileEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("profileForm") ProfileEditRequest request,
            BindingResult bindingResult,
            @RequestParam(name = "profileImage", required = false)
            MultipartFile profileImage,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        if (!bindingResult.hasErrors()) {
            try {
                myPageService.updateProfile(userDetails.getUserId(), request, profileImage);
                return "redirect:/my/profile-edit?saved=true";
            } catch (MyPageException exception) {
                if (exception.getField() != null && !"profileImage".equals(exception.getField())) {
                    bindingResult.rejectValue(
                            exception.getField(),
                            "profile.edit",
                            exception.getMessage()
                    );
                } else {
                    bindingResult.reject("profile.edit", exception.getMessage());
                }
            }
        }

        User user = myPageService.getUser(userDetails.getUserId());
        PersonalProfile profile = resolveProfile(model, userDetails.getUserId());
        addProfileEditModel(model, userDetails.getUserId(), user, profile);
        return "my/profile-edit";
    }

    /**
     * {@link MySidebarAdvice}가 이미 같은 요청에서 조회해둔 PersonalProfile이 있으면 재사용하고,
     * 없으면(예: 사이드바 대상이 아닌 경우) 새로 조회합니다.
     */
    private PersonalProfile resolveProfile(Model model, Long userId) {
        if (model.getAttribute("personalProfile") instanceof PersonalProfile cachedProfile) {
            return cachedProfile;
        }
        return myPageService.getProfile(userId);
    }

    private void addProfileEditModel(
            Model model,
            Long userId,
            User user,
            PersonalProfile profile
    ) {
        model.addAttribute("activeMenu", "edit");
        model.addAttribute("email", user.getEmail());
        model.addAttribute("profileImageUrl", resolveProfileImageUrl(model, profile));
        model.addAttribute("desiredJobs", myPageService.getDesiredJobs(userId));
        model.addAttribute(
                "jobCategoryGroups",
                jobService.getActiveJobCategoryGroups()
        );
    }

    /**
     * {@link MySidebarAdvice}가 이미 같은 요청에서 발급해둔 presigned URL이 있으면 재사용해서,
     * S3Presigner 서명 계산과 DB 조회가 요청당 두 번씩 일어나지 않도록 합니다.
     */
    private String resolveProfileImageUrl(Model model, PersonalProfile profile) {
        if (model.containsAttribute("personalProfile")) {
            return (String) model.getAttribute("mySidebarProfileImageUrl");
        }
        return myPageService.getProfileImageDisplayUrl(profile);
    }

    @PostMapping("/password-change")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MyPasswordChangeRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    MyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            myPageService.changePassword(userDetails.getUserId(), request);
            return ResponseEntity.ok(MyAccountActionResponse.success(
                    "비밀번호가 변경되었습니다."
            ));
        } catch (MyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    MyAccountActionResponse.failure(exception.getMessage())
            );
        }
    }

    @PostMapping("/withdraw")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MyWithdrawalRequest request,
            HttpServletRequest httpRequest
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    MyAccountActionResponse.failure(
                            "로그인이 만료되었습니다. 다시 로그인해주세요."
                    )
            );
        }
        try {
            myPageService.withdraw(userDetails.getUserId(), request.currentPassword());
        } catch (MyAccountException exception) {
            return ResponseEntity.badRequest().body(
                    MyAccountActionResponse.failure(exception.getMessage())
            );
        }

        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(MyAccountActionResponse.success(
                "회원 탈퇴가 완료되었습니다.",
                "/auth/login?accountStatus=withdrawn"
        ));
    }

    @GetMapping("/applications")
    public String applications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        var result = myApplicationQueryService.getApplications(
                userDetails.getUserId(),
                filter,
                page
        );

        model.addAttribute("activeMenu", "applications");
        model.addAttribute("applications", result.applications());
        model.addAttribute("applicationCounts", result.counts());
        model.addAttribute("filter", result.filter());
        model.addAttribute("pageHandler", result.pageHandler());
        return "my/applications";
    }

    @GetMapping("/applications/{applicationNo}")
    public String applicationDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationNo,
            Model model
    ) {
        if (userDetails == null) return "redirect:/auth/login";
        model.addAttribute("applicationDetail", myApplicationDetailService.getDetail(
                userDetails.getUserId(),
                applicationNo
        ));
        model.addAttribute("activeMenu", "applications");
        return "my/application-detail";
    }

    @PostMapping("/applications/{applicationNo}/cancel")
    public String cancelApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationNo,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            myApplicationDetailService.cancel(
                    userDetails.getUserId(),
                    applicationNo
            );
            redirectAttributes.addFlashAttribute(
                    "applicationSuccessMessage",
                    "지원을 취소했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "applicationErrorMessage",
                    exception.getMessage()
            );
        }
        return "redirect:/my/applications/" + applicationNo;
    }

    @GetMapping("/saved-jobs")
    public String savedJobs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        if (userDetails == null) return "redirect:/auth/login";
        if (!java.util.Set.of("all", "open", "deadline").contains(filter)) filter = "all";
        int pageSize = 4;
        int total = myPageService.getSavedJobCount(userDetails.getUserId(), filter);
        PageHandler pageHandler = new PageHandler(Math.max(page, 1), total, pageSize);
        model.addAttribute("savedJobs", myPageService.getSavedJobList(
                userDetails.getUserId(), filter, pageHandler.getOffset(), pageSize));
        model.addAttribute("savedJobCount", total);
        model.addAttribute("filter", filter);
        model.addAttribute("pageHandler", pageHandler);
        return "my/saved-jobs";
    }

    @PostMapping("/saved-jobs/remove")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> removeSavedJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long jobPostingId
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MyAccountActionResponse.failure("로그인이 필요합니다."));
        }
        try {
            myPageService.removeSavedJob(userDetails.getUserId(), jobPostingId);
            return ResponseEntity.ok(MyAccountActionResponse.success("관심 공고에서 해제되었습니다."));
        } catch (MyPageException e) {
            return ResponseEntity.badRequest().body(MyAccountActionResponse.failure(e.getMessage()));
        }
    }

    @GetMapping("/saved-companies")
    public String savedCompanies(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        if (userDetails == null) return "redirect:/auth/login";
        if (!java.util.Set.of("recent", "name", "jobs").contains(sort)) sort = "recent";
        int pageSize = 6;
        int total = myPageService.getSavedCompanyCount(userDetails.getUserId());
        PageHandler pageHandler = new PageHandler(Math.max(page, 1), total, pageSize);
        model.addAttribute("savedCompanies", myPageService.getSavedCompanyList(
                userDetails.getUserId(), sort, pageHandler.getOffset(), pageSize));
        model.addAttribute("savedCompanyCount", total);
        model.addAttribute("sort", sort);
        model.addAttribute("pageHandler", pageHandler);
        return "my/saved-companies";
    }

    @PostMapping("/saved-companies/remove")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> removeSavedCompany(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long companyId
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MyAccountActionResponse.failure("로그인이 필요합니다."));
        }
        try {
            myPageService.removeSavedCompany(userDetails.getUserId(), companyId);
            return ResponseEntity.ok(MyAccountActionResponse.success("관심 기업에서 해제되었습니다."));
        } catch (MyPageException e) {
            return ResponseEntity.badRequest().body(MyAccountActionResponse.failure(e.getMessage()));
        }
    }

    @GetMapping("/reviews")
    public String reviews(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam(defaultValue = "company") String type,
                          Model model) {
        if (userDetails == null) return "redirect:/auth/login";
        if (!java.util.Set.of("company", "interview").contains(type)) type = "company";
        Long userId = userDetails.getUserId();
        model.addAttribute("companyReviews", myPageService.getMyCompanyReviews(userId));
        model.addAttribute("interviewReviews", myPageService.getMyInterviewReviews(userId));
        model.addAttribute("companyReviewCount", myPageService.getMyCompanyReviewCount(userId));
        model.addAttribute("interviewReviewCount", myPageService.getMyInterviewReviewCount(userId));
        model.addAttribute("type", type);
        return "my/reviews";
    }

    @GetMapping("/reviews/company/edit")
    public String editCompanyReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam Long reviewId, Model model,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            CompanyReviewsDTO dto = myPageService.getMyCompanyReview(userDetails.getUserId(), reviewId);
            model.addAttribute("companyReviewsDTO", dto);
            model.addAttribute("companyName", dto.getCompanyName());
            model.addAttribute("jobCategoryName", dto.getJobCategoryName());
            model.addAttribute("formAction", "/my/reviews/company/edit");
            model.addAttribute("isEdit", true);
            return "company/review-write";
        } catch (MyPageException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my/reviews?type=company";
        }
    }

    @PostMapping("/reviews/company/edit")
    public String editCompanyReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @ModelAttribute CompanyReviewsDTO dto,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            myPageService.updateMyCompanyReview(userDetails.getUserId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "기업리뷰가 수정되었습니다.");
            return "redirect:/my/reviews?type=company";
        } catch (MyPageException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my/reviews/company/edit?reviewId=" + dto.getCompanyReviewId();
        }
    }

    @GetMapping("/reviews/interview/edit")
    public String editInterviewReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @RequestParam Long reviewId, Model model,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            InterviewReviewsDTO dto = myPageService.getMyInterviewReview(userDetails.getUserId(), reviewId);
            model.addAttribute("interviewReviewsDTO", dto);
            model.addAttribute("formAction", "/my/reviews/interview/edit");
            model.addAttribute("isEdit", true);
            return "company/interview-review-write";
        } catch (MyPageException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my/reviews?type=interview";
        }
    }

    @PostMapping("/reviews/interview/edit")
    public String editInterviewReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @ModelAttribute InterviewReviewsDTO dto,
                                      RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/auth/login";
        try {
            myPageService.updateMyInterviewReview(userDetails.getUserId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "면접후기가 수정되었습니다.");
            return "redirect:/my/reviews?type=interview";
        } catch (MyPageException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my/reviews/interview/edit?reviewId=" + dto.getInterviewReviewId();
        }
    }

    @PostMapping("/reviews/delete")
    @ResponseBody
    public ResponseEntity<MyAccountActionResponse> deleteMyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String type, @RequestParam Long reviewId) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MyAccountActionResponse.failure("로그인이 필요합니다."));
        try {
            myPageService.deleteMyReview(userDetails.getUserId(), type, reviewId);
            return ResponseEntity.ok(MyAccountActionResponse.success("후기가 삭제되었습니다."));
        } catch (MyPageException e) {
            return ResponseEntity.badRequest().body(MyAccountActionResponse.failure(e.getMessage()));
        }
    }
}
