package kr.co.firstdayproject.controller;

import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
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
    public String index(Model model) {
        // 등록된 배너 조회
        model.addAttribute("banners",
                bannerService.getActiveBanners());
        // 채용공고 최신순
        model.addAttribute("latestJobs",
                jobService.getLatestJobPostingList());
        // 채용공고 인기순
        model.addAttribute("popularJobs",
                jobService.getPopularJobPostingList());
        // 인기 기업
        model.addAttribute("popularCompanies",
                companyService.getPopularCompanyList());

        return "index";
    }
}
