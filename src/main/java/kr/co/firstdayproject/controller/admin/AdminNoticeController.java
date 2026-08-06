package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.cs.NoticeDto;
import kr.co.firstdayproject.service.cs.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 - 고객센터 > 공지사항 관리
 * 화면 1개(admin/cs/notice.html)에서 목록 + 등록/수정 폼(모달)을 모두 처리한다.
 * 등록/수정/삭제/고정토글은 JSON 응답하는 API로 동작 (JS fetch에서 호출).
 */
@Controller
@RequestMapping("/admin/cs/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    private static final int PAGE_SIZE = 10;

    private final NoticeService noticeService;

    // 목록 페이지 (화면 1개)
    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE);
        Page<NoticeDto.ListItem> noticePage = noticeService.getAdminList(keyword, pageable);

        model.addAttribute("activeMenu", "cs");
        model.addAttribute("noticeList", noticePage.getContent());
        model.addAttribute("noticePage", noticePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        return "admin/cs/notice";
    }

    // 상세 조회 (수정 모달 프리필용, JSON)
    @GetMapping("/{id}")
    @ResponseBody
    public NoticeDto.Detail detail(@PathVariable Long id) {
        return noticeService.getDetail(id);
    }

    // 등록 (JSON)
    @PostMapping
    @ResponseBody
    public Long create(@RequestBody NoticeDto.SaveRequest req) {
        Long adminId = 1L; // TODO: 로그인 관리자 ID로 교체
        return noticeService.create(req, adminId);
    }

    // 수정 (JSON)
    @PostMapping("/{id}")
    @ResponseBody
    public void update(@PathVariable Long id, @RequestBody NoticeDto.SaveRequest req) {
        noticeService.update(id, req);
    }

    // 삭제
    @PostMapping("/{id}/delete")
    @ResponseBody
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }

    // 고정 토글
    @PostMapping("/{id}/pin")
    @ResponseBody
    public void togglePin(@PathVariable Long id) {
        noticeService.togglePin(id);
    }
}