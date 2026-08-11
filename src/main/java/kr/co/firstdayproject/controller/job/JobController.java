package kr.co.firstdayproject.controller.job;

import kr.co.firstdayproject.dto.common.PageInfo;
import kr.co.firstdayproject.dto.job.JobListItem;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
import java.util.List;
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
    private final BannerService bannerService;

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
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "latest",
                        page,
                        authentication
                );

        addJobListAttributes(model, jobPage);
        addSearchConditionAttributes(
                model,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "latest"
        );

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
            List<Long> categoryId,

            @RequestParam(required = false)
            List<String> region,

            @RequestParam(required = false)
            List<String> career,

            @RequestParam(required = false)
            List<String> education,

            @RequestParam(required = false)
            List<Long> skillId,

            @RequestParam(defaultValue = "latest")
            String sort,

            @RequestParam(defaultValue = "0")
            int page,

            Authentication authentication,
            Model model
    ) {
        String normalizedSort = normalizeSort(sort);

        Page<JobListItem> jobPage =
                jobService.getRecruitingJobPostingList(
                        keyword,
                        parentCategoryId,
                        categoryId,
                        region,
                        career,
                        education,
                        skillId,
                        normalizedSort,
                        page,
                        authentication
                );

        addJobListAttributes(model, jobPage);

        // 검색 결과 화면에서 입력한 조건 유지
        model.addAttribute(
                "parentCategoryId",
                parentCategoryId
        );
        addSearchConditionAttributes(
                model,
                keyword,
                categoryId,
                region,
                career,
                education,
                skillId,
                normalizedSort
        );

        return "job/list";
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
        model.addAttribute(
                "skillGroups",
                jobService.getActiveSkillGroups()
        );
        model.addAttribute(
                "jobBanner",
                bannerService.getActiveBanners("job")
                        .stream()
                        .findFirst()
                        .orElse(null)
        );
    }

    private void addSearchConditionAttributes(
            Model model,
            String keyword,
            List<Long> categoryIds,
            List<String> regions,
            List<String> careers,
            List<String> educations,
            List<Long> skillIds,
            String sort
    ) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryIds", emptyIfNull(categoryIds));
        model.addAttribute("regions", emptyIfNull(regions));
        model.addAttribute("careers", emptyIfNull(careers));
        model.addAttribute("educations", emptyIfNull(educations));
        model.addAttribute("skillIds", emptyIfNull(skillIds));
        model.addAttribute("sort", sort);
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String normalizeSort(String sort) {
        if ("deadline".equals(sort) || "views".equals(sort)) {
            return sort;
        }

        return "latest";
    }

    /**
     * 채용공고 상세 화면.
     *
     * 상세 조회 기능은 이후 Service와 연결한다.
     */
    @GetMapping("/detail")
    public String detail(
            @RequestParam Long jobPostingId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long parentCategoryId,
            @RequestParam(required = false) List<Long> categoryId,
            @RequestParam(required = false) List<String> region,
            @RequestParam(required = false) List<String> career,
            @RequestParam(required = false) List<String> education,
            @RequestParam(required = false) List<Long> skillId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        String normalizedSort = normalizeSort(sort);
        Page<JobListItem> jobPage = jobService.getRecruitingJobPostingDetailList(
                keyword,
                parentCategoryId,
                categoryId,
                region,
                career,
                education,
                skillId,
                normalizedSort,
                page,
                authentication
        );

        addJobListAttributes(model, jobPage);
        addSearchConditionAttributes(
                model,
                keyword,
                categoryId,
                region,
                career,
                education,
                skillId,
                normalizedSort
        );
        model.addAttribute("parentCategoryId", parentCategoryId);
        model.addAttribute(
                "job",
                jobService.getRecruitingJobPosting(
                        jobPostingId,
                        authentication
                )
        );
        model.addAttribute(
                "personalMember",
                authentication != null
                        && authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                "ROLE_PERSONAL".equals(authority.getAuthority())
                        )
        );
        model.addAttribute(
                "categoryGroups",
                jobService.getActiveJobCategoryGroups()
        );
        model.addAttribute(
                "skillGroups",
                jobService.getActiveSkillGroups()
        );

        return "job/detail";
    }
}
