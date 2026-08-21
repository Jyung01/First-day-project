package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.admin.InquiryDetailDto;
import kr.co.firstdayproject.dto.admin.InquiryListItemDto;
import kr.co.firstdayproject.entity.cs.InquiryAttachment;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.cs.FaqService;
import kr.co.firstdayproject.service.cs.NoticeService;
import kr.co.firstdayproject.service.cs.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Map;

/**
 * 관리자 - 고객센터 관리 > 1:1 문의
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/cs/qna")
public class AdminQnaController {

    private final QnaService qnaService;
    private final NoticeService noticeService;
    private final FaqService faqService;

    /** 1:1 문의 관리 목록 화면 */
    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String memberType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        Page<InquiryListItemDto> result =
                qnaService.getInquiryList(
                        categoryId,
                        memberType,
                        status,
                        keyword,
                        PageRequest.of(page, 10)
                );

        model.addAttribute("activeMenu", "cs");
        model.addAttribute("inquiries", result.getContent());
        model.addAttribute("categories", qnaService.getActiveCategories());
        model.addAttribute("totalCount", result.getTotalElements());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("memberType", memberType);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        // 상단 통계 카드(미답변 문의/공지사항/FAQ) - 3개 화면 공통, 항상 세 값을 다 채운다.
        model.addAttribute("pendingCount", qnaService.getPendingCount());
        model.addAttribute("noticeCount", noticeService.getTotalCount());
        model.addAttribute("faqCount", faqService.getTotalCount());

        return "admin/cs/qna";
    }

    /** 문의 상세 조회 (답변 모달을 열 때 AJAX로 호출) */
    @GetMapping("/{inquiryId}")
    @ResponseBody
    public InquiryDetailDto detail(@PathVariable Long inquiryId) {
        return qnaService.getInquiryDetail(inquiryId);
    }

    /** 관리자 - 문의 첨부파일 다운로드 */
    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        InquiryAttachment attachment = qnaService.getAttachmentForAdmin(attachmentId);
        String downloadUrl = qnaService.getAttachmentDownloadUrl(attachment);
        if (downloadUrl != null) {
            return ResponseEntity.status(302)
                    .location(URI.create(downloadUrl))
                    .build();
        }

        Resource resource = new FileSystemResource(attachment.getStorageKey());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    /** 답변 등록 */
    @PostMapping("/{inquiryId}/answer")
    @ResponseBody
    public ResponseEntity<Void> registerAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long inquiryId,
            @RequestBody Map<String, String> body
    ) {
        Long adminId = AdminPrincipal.requireAdminId(userDetails);
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
