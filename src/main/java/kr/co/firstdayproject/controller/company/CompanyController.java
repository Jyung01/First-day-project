package kr.co.firstdayproject.controller.company;

import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping({"", "/list"})
    public String list(@RequestParam(defaultValue = "1") int page,
                       CompanySearchDTO search,
                       Model model) {

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

        return "company/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam Long companyId,
                         Model model) {

        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));
        return "company/detail";
    }

    @GetMapping("/jobs")
    public String jobs() {
        return "company/jobs";
    }
}
