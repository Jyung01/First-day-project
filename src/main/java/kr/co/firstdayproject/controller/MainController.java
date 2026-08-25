package kr.co.firstdayproject.controller;

import java.util.List;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.job.JobService;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
public class MainController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final BannerService bannerService;

    @GetMapping("/")
    public String index(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        model.addAttribute("latestJobs",
                jobService.getLatestJobPostingList());
        model.addAttribute("popularJobs",
                jobService.getPopularJobPostingList());
        model.addAttribute("popularCompanies",
                companyService.getPopularCompanyList());
        model.addAttribute("banners",
                bannerService.getActiveBanners("main"));

        List<JobCategoryGroup> jobCategoryGroups = jobService.getActiveJobCategoryGroups();
        model.addAttribute("jobCategoryGroups", jobCategoryGroups);
        model.addAttribute("quickJobCategory", jobCategoryGroups.stream()
                .filter(group -> group.categoryName().contains("IT")
                        || group.categoryName().contains("개발"))
                .findFirst()
                .orElse(null));

        boolean personalMember = userDetails != null
                && "개인".equals(userDetails.getUserType());
        boolean companyMember = userDetails != null
                && "기업".equals(userDetails.getUserType());
        model.addAttribute("personalMember", personalMember);
        model.addAttribute("companyMember", companyMember);

        return "index";
    }
}
