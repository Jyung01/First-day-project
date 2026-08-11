package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.admin.company.AdminCompanyDetail;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyListView;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyReviewRequest;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminCompanyReviewException;
import kr.co.firstdayproject.service.admin.AdminCompanyService;
import kr.co.firstdayproject.service.admin.MemberSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/company")
public class AdminCompanyController {

    private final AdminCompanyService adminCompanyService;
    private final MemberSessionService memberSessionService;

    @GetMapping("/{companyId}")
    @ResponseBody
    public ResponseEntity<AdminCompanyDetail> detail(
            @PathVariable Long companyId
    ) {
        try {
            return ResponseEntity.ok(
                    adminCompanyService.getCompanyDetail(companyId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{companyId}/approve")
    @ResponseBody
    public ResponseEntity<AdminCompanyDetail> approve(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable Long companyId
    ) {
        try {
            return ResponseEntity.ok(
                    adminCompanyService.approveCompany(
                            admin == null ? null : admin.getUserId(),
                            companyId
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | OptimisticLockingFailureException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{companyId}/reject")
    @ResponseBody
    public ResponseEntity<AdminCompanyDetail> reject(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable Long companyId,
            @RequestBody(required = false) AdminCompanyReviewRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    adminCompanyService.rejectCompany(
                            admin == null ? null : admin.getUserId(),
                            companyId,
                            request
                    )
            );
        } catch (AdminCompanyReviewException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | OptimisticLockingFailureException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{companyId}/suspend")
    @ResponseBody
    public ResponseEntity<AdminCompanyDetail> suspend(
            @PathVariable Long companyId
    ) {
        try {
            AdminCompanyDetail company = adminCompanyService.suspendCompany(
                    companyId
            );
            memberSessionService.expireAllCompanySessions(companyId);
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | OptimisticLockingFailureException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{companyId}/unsuspend")
    @ResponseBody
    public ResponseEntity<AdminCompanyDetail> unsuspend(
            @PathVariable Long companyId
    ) {
        try {
            return ResponseEntity.ok(
                    adminCompanyService.unsuspendCompany(companyId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | OptimisticLockingFailureException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        AdminCompanyListView listView = adminCompanyService.getCompanyList(
                status,
                keyword,
                page
        );
        int currentPage = listView.companyPage().getNumber() + 1;
        int totalPages = listView.companyPage().getTotalPages();
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + 4);
        startPage = Math.max(1, endPage - 4);

        model.addAttribute("activeMenu", "company");
        model.addAttribute("companyPage", listView.companyPage());
        model.addAttribute("statistics", listView.statistics());
        model.addAttribute("selectedStatus", listView.selectedStatus());
        model.addAttribute("keyword", listView.keyword());
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "admin/company/index";
    }
}
