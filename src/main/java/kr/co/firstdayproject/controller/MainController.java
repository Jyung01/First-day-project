package kr.co.firstdayproject.controller;

import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.job.JobService;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    public final BannerService bannerService;
    private final JobService jobService;
    private final CompanyService companyService;

    @GetMapping("/")
    public String index(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        // 등록된 배너 조회
        model.addAttribute("banners",
                bannerService.getActiveBanners("main"));
        // 채용공고 최신순
        model.addAttribute("latestJobs",
                jobService.getLatestJobPostingList());
        // 채용공고 인기순
        model.addAttribute("popularJobs",
                jobService.getPopularJobPostingList());
        boolean personalMember = userDetails != null
                && "개인".equals(userDetails.getUserType());
        boolean companyMember = userDetails != null
                && "기업".equals(userDetails.getUserType());
        java.util.Map<Long, Integer> aiMatchScores = personalMember
                ? jobService.getPersonalizedJobMatchScores(userDetails.getUserId())
                : java.util.Map.of();
        List<kr.co.firstdayproject.dto.job.MainJobListItem> personalizedJobs = personalMember
                ? jobService.getPersonalizedJobPostingList(userDetails.getUserId(), aiMatchScores)
                : List.of();
        model.addAttribute("personalizedJobs", personalizedJobs);
        model.addAttribute("aiMatchScores", aiMatchScores);
        model.addAttribute("aiMatchReasons", personalMember
                ? jobService.getPersonalizedJobMatchReasons(userDetails.getUserId(), personalizedJobs, aiMatchScores)
                : java.util.Map.of());
        model.addAttribute("personalMember", personalMember);
        model.addAttribute("companyMember", companyMember);
        // 인기 기업
        model.addAttribute("popularCompanies",
                companyService.getPopularCompanyList());

        return "index";
    }
}
