package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.admin.member.AdminMemberDetail;
import kr.co.firstdayproject.dto.admin.member.AdminMemberListView;
import kr.co.firstdayproject.service.admin.AdminMemberService;
import kr.co.firstdayproject.service.admin.MemberSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/member")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final MemberSessionService memberSessionService;

    @GetMapping("/{memberId}")
    @ResponseBody
    public ResponseEntity<AdminMemberDetail> detail(
            @PathVariable Long memberId
    ) {
        try {
            return ResponseEntity.ok(
                    adminMemberService.getMemberDetail(memberId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{memberId}/suspend")
    @ResponseBody
    public ResponseEntity<AdminMemberDetail> suspend(
            @PathVariable Long memberId
    ) {
        try {
            AdminMemberDetail member = adminMemberService.suspendMember(memberId);
            memberSessionService.expireAllSessions(memberId);
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{memberId}/unsuspend")
    @ResponseBody
    public ResponseEntity<AdminMemberDetail> unsuspend(
            @PathVariable Long memberId
    ) {
        try {
            return ResponseEntity.ok(
                    adminMemberService.unsuspendMember(memberId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        AdminMemberListView listView = adminMemberService.getMemberList(
                status,
                keyword,
                page
        );
        int currentPage = listView.memberPage().getNumber() + 1;
        int totalPages = listView.memberPage().getTotalPages();
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + 4);
        startPage = Math.max(1, endPage - 4);

        model.addAttribute("activeMenu", "member");
        model.addAttribute("memberPage", listView.memberPage());
        model.addAttribute("statistics", listView.statistics());
        model.addAttribute("selectedStatus", listView.selectedStatus());
        model.addAttribute("keyword", listView.keyword());
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "admin/member/index";
    }
}
