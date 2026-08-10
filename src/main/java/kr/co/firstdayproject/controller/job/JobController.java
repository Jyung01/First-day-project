package kr.co.firstdayproject.controller.job;

import kr.co.firstdayproject.dto.common.PageInfo;
import kr.co.firstdayproject.dto.job.JobListItem;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * 전체 모집 중 채용공고 목록.
     *
     * 요청 예시:
     * /job
     * /job/list
     * /job/list?page=1
     */
    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        Page<JobListItem> jobPage =
                jobService.getRecruitingJobPostingList(
                        null,
                        null,
                        null,
                        page,
                        authentication
                );

        addJobListAttributes(model, jobPage);

        return "job/list";
    }

    /**
     * 채용공고 검색 및 필터 결과.
     *
     * 요청 예시:
     * /job/search?keyword=Java
     * /job/search?parentCategoryId=1
     * /job/search?categoryId=2
     */
    @GetMapping("/search")
    public String search(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long parentCategoryId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(defaultValue = "0")
            int page,

            Authentication authentication,
            Model model
    ) {
        Page<JobListItem> jobPage =
                jobService.getRecruitingJobPostingList(
                        keyword,
                        parentCategoryId,
                        categoryId,
                        page,
                        authentication
                );

        addJobListAttributes(model, jobPage);

        // 검색 결과 화면에서 입력한 조건 유지
        model.addAttribute("keyword", keyword);
        model.addAttribute(
                "parentCategoryId",
                parentCategoryId
        );
        model.addAttribute("categoryId", categoryId);

        return "job/search";
    }

    /**
     * 목록과 검색 결과 화면에서 공통으로 사용하는 Model 데이터.
     */
    private void addJobListAttributes(
            Model model,
            Page<JobListItem> jobPage
    ) {
        model.addAttribute("jobPage", jobPage);
        model.addAttribute(
                "jobs",
                jobPage.getContent()
        );

        model.addAttribute(
                "pageInfo",
                PageInfo.of(jobPage)
        );

        model.addAttribute(
                "categoryGroups",
                jobService.getActiveJobCategoryGroups()
        );
    }

    /**
     * 채용공고 상세 화면.
     *
     * 상세 조회 기능은 이후 Service와 연결한다.
     */
    @GetMapping("/detail")
    public String detail(
            @RequestParam Long jobPostingId,
            Model model
    ) {
        model.addAttribute(
                "jobPostingId",
                jobPostingId
        );

        return "job/detail";
    }
}