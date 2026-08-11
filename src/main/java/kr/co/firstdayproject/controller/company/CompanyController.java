package kr.co.firstdayproject.controller.company;

import jakarta.servlet.http.HttpSession;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.dto.job.JobDTO;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.job.SavedJobService;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;
    private final BannerService bannerService;
    private final SavedJobService savedJobService;

    @GetMapping({"", "/list"})
    public String list(@RequestParam(defaultValue = "1") int page,
                       CompanySearchDTO search,
                       Model model,
                       Authentication authentication) {
        
        // 로그인 사용자 ID
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            search.setUserId(userDetails.getUserId());
        }

        // 검색 결과 전체 개수
        int total = companyService.getCompanyCount(search);

        // 페이지 계산
        PageHandler ph = new PageHandler(page, total, 6);

        // 검색 DTO에 페이징 정보 저장
        search.setOffset(ph.getOffset());
        search.setPageSize(ph.getPageSize());

        // 목록 조회
        model.addAttribute("companyList",
                companyService.getCompanyList(search));

        model.addAttribute("ph", ph);
        model.addAttribute("companyCount", total);
        model.addAttribute("search", search);

        model.addAttribute("industryList",
                companyService.getIndustryList());

        model.addAttribute("regionList",
                companyService.getRegionList());

        model.addAttribute("companySizeList",
                companyService.getCompanySizeList());

       model.addAttribute("jobCategoryList",
               companyService.getJobCategoryList());

        model.addAttribute("companyBanner",
                bannerService.getActiveBanners("companies")
                        .stream()
                        .findFirst()
                        .orElse(null));



        return "company/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam Long companyId,
                         Model model,
                         Authentication authentication) {

        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));

        model.addAttribute("jobPostingList",
                companyService.getCompanyJobPostingList(companyId));

        model.addAttribute("isWished", isWished(authentication, companyId));
        model.addAttribute("loggedIn",
                authentication != null
                        && authentication.isAuthenticated()
                        && authentication.getPrincipal() instanceof CustomUserDetails);

        return "company/detail";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam Long companyId, Model model, Authentication authentication) {
        List<JobDTO> recruitList = companyService.getCompanyRecruitList(companyId);

        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));
        model.addAttribute("recruitList", recruitList);
        model.addAttribute("savedJobPostingIds",
                savedJobService.getSavedJobPostingIds(
                        recruitList.stream().map(JobDTO::getJobPostingId).toList(),
                        authentication));
        model.addAttribute("personalMember",
                authentication != null
                        && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_PERSONAL".equals(authority.getAuthority())));
        model.addAttribute("isWished", isWished(authentication, companyId));
        return "company/jobs";
    }

    private boolean isWished(Authentication authentication, Long companyId) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return companyService.isWished(userDetails.getUserId(), companyId);
        }
        return false;
    }

    // 기업 정보 : 관심기업 등록 및 해제
    @PostMapping("/wish/{companyId}")
    @ResponseBody
    public Map<String, Object> toggleWish(
            @PathVariable Long companyId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {

            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        Long userId = userDetails.getUserId();

        boolean wished = companyService.toggleWish(userId, companyId);

        response.put("success", true);
        response.put("wished", wished);

        return response;
    }


}
