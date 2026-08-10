package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.admin.InquiryDetailDto;
import kr.co.firstdayproject.dto.admin.InquiryListItemDto;
import kr.co.firstdayproject.service.cs.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 - 고객센터 관리 > 1:1 문의
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cs/qna")
public class AdminQnaController {

    private final QnaService qnaService;

    /** 1:1 문의 관리 목록 화면 */
    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<InquiryListItemDto> result =
                qnaService.getInquiryList(categoryId, status, keyword, PageRequest.of(page, 10));

        model.addAttribute("inquiries", result.getContent());
        model.addAttribute("categories", qnaService.getActiveCategories());
        model.addAttribute("totalCount", result.getTotalElements());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", qnaService.getPendingCount());

        return "admin/cs/qna";
    }

    /** 문의 상세 조회 (답변 모달을 열 때 AJAX로 호출) */
    @GetMapping("/{inquiryId}")
    @ResponseBody
    public InquiryDetailDto detail(@PathVariable Long inquiryId) {
        return qnaService.getInquiryDetail(inquiryId);
    }

    /** 답변 등록 */
    @PostMapping("/{inquiryId}/answer")
    @ResponseBody
    public ResponseEntity<Void> registerAnswer(
            @PathVariable Long inquiryId,
            @RequestBody Map<String, String> body
    ) {
        // TODO: 관리자 로그인/세션 연동 후 실제 로그인한 관리자 ID로 교체
        Long adminId = 1L;
        qnaService.answerInquiry(inquiryId, body.get("answerContent"), adminId);
        return ResponseEntity.ok().build();
    }

    /** 답변 수정 */
    @PutMapping("/{inquiryId}/answer")
    @ResponseBody
    public ResponseEntity<Void> updateAnswer(
            @PathVariable Long inquiryId,
            @RequestBody Map<String, String> body
    ) {
        qnaService.updateAnswer(inquiryId, body.get("answerContent"));
        return ResponseEntity.ok().build();
    }

    /** 답변 삭제 (미답변 상태로 되돌림) */
    @DeleteMapping("/{inquiryId}/answer")
    @ResponseBody
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long inquiryId) {
        qnaService.deleteAnswer(inquiryId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(404).body(Map.of("message", ex.getMessage()));
    }
}