package kr.co.firstdayproject.controller.my;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewDetail;
import kr.co.firstdayproject.dto.common.PageInfo;
import kr.co.firstdayproject.dto.job.JobListItem;
import kr.co.firstdayproject.dto.my.CoverLetterDto;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import kr.co.firstdayproject.service.job.JobService;
import kr.co.firstdayproject.service.my.CoverLetterAiReviewService;
import kr.co.firstdayproject.service.my.CoverLetterService;
import kr.co.firstdayproject.service.my.MyPageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import kr.co.firstdayproject.security.CustomUserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/my/cover-letter")
public class CoverLetterController {

    private static final Logger log = LoggerFactory.getLogger(CoverLetterController.class);
    private static final int SAVED_JOB_PICKER_LIMIT = 5;

    private final CoverLetterService coverLetterService;
    private final CoverLetterAiReviewService coverLetterAiReviewService;
    private final MyPageService myPageService;
    private final JobService jobService;

    @GetMapping({"", "/list"})
    public String list(Model model, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetters", coverLetterService.findMyList(userId));
        return "my/cover-letter/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        if (id == null) {
            return "redirect:/my/cover-letter/list";
        }

        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        model.addAttribute("coverLetterItems", coverLetterService.getItems(id, userId));
        return "my/cover-letter/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        model.addAttribute("activeMenu", "coverLetters");

        if (id != null) {
            Long userId = getCurrentUserId(request);
            CoverLetter letter = coverLetterService.getMine(id, userId);
            model.addAttribute("coverLetter", letter);
            model.addAttribute("coverLetterItems", coverLetterService.getItems(id, userId));
        }
        return "my/cover-letter/form";
    }

    @GetMapping("/ai-result")
    public String aiResult(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Boolean reselect,
            Model model,
            HttpServletRequest request
    ) {
        if (id == null) {
            return "redirect:/my/cover-letter/list";
        }

        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);
        List<CoverLetterItem> items = coverLetterService.getItems(id, userId);

        Optional<CoverLetterAiReviewDetail> latestReview =
                coverLetterAiReviewService.getLatestReview(id, userId);
        // reselect=true면 이미 첨삭 이력이 있어도 공고 재선택 화면(상태 A)을 보여준다.
        boolean hasAiReview = latestReview.isPresent() && !Boolean.TRUE.equals(reselect);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        model.addAttribute("coverLetterItems", items);
        model.addAttribute("hasAiReview", hasAiReview);
        latestReview.ifPresent(detail -> model.addAttribute("latestReview", detail));

        // 첨삭 대상 공고 선택 화면(상태 A)에 필요한 데이터.
        // 관심 등록한 모집중 공고를 우선 노출하고, 검색은 항상 열려있게 한다.
        model.addAttribute(
                "savedJobs",
                myPageService.getSavedJobList(userId, "open", 0, SAVED_JOB_PICKER_LIMIT)
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Page<JobListItem> jobSearchResults = jobService.getJobPostingPickerList(
                keyword,
                page,
                authentication
        );
        model.addAttribute("jobSearchResults", jobSearchResults.getContent());
        model.addAttribute("jobSearchPageInfo", PageInfo.of(jobSearchResults));
        model.addAttribute("keyword", keyword);

        return "my/cover-letter/ai-result";
    }

    /**
     * fetch()로 호출되는 AJAX 엔드포인트. 문항 수만큼 OpenAI를 순차 호출해 응답까지
     * 몇 초~몇십 초 걸릴 수 있어, 프런트는 이 응답을 기다리는 동안 로딩 오버레이를 보여준다.
     */
    @PostMapping("/{id}/ai-reviews")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestAiReview(
            @PathVariable Long id,
            @RequestParam Long jobPostingId,
            HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        try {
            coverLetterAiReviewService.requestReview(id, userId, jobPostingId);
        } catch (Exception exception) {
            log.error(
                    "자소서 AI 첨삭 생성 실패: coverLetterId={}, jobPostingId={}",
                    id,
                    jobPostingId,
                    exception
            );
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "message",
                            "AI 첨삭 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "redirectUrl",
                "/my/cover-letter/ai-result?id=" + id
        ));
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Long> create(
            @RequestBody CoverLetterDto.CreateRequest request,
            HttpServletRequest servletRequest
    ) {
        Long userId = getCurrentUserId(servletRequest);
        Long id = coverLetterService.create(userId, request);
        return ResponseEntity.ok(id);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody CoverLetterDto.CreateRequest request,
            HttpServletRequest servletRequest
    ) {
        Long userId = getCurrentUserId(servletRequest);
        coverLetterService.update(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        coverLetterService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}