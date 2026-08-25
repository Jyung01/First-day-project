package kr.co.firstdayproject.service.cs;

import kr.co.firstdayproject.dto.cs.NoticeDto;
import kr.co.firstdayproject.entity.cs.Notice;
import kr.co.firstdayproject.repository.cs.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /** 사용자 화면: '공개' 상태만 노출 */
    public Page<NoticeDto.ListItem> getUserList(String keyword, Pageable pageable) {
        return noticeRepository.search(Notice.STATUS_PUBLISHED, keyword, pageable)
                .map(NoticeDto.ListItem::from);
    }

    /** 관리자 화면: 상태 상관없이 전체 노출 */
    public Page<NoticeDto.ListItem> getAdminList(String keyword, Pageable pageable) {
        return noticeRepository.search(null, keyword, pageable)
                .map(NoticeDto.ListItem::from);
    }

    // 관리자 대시보드 상단 통계용 - 공지사항 전체 건수(상태 무관)
    public long getTotalCount() {
        return noticeRepository.count();
    }

    public NoticeDto.Detail getDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. id=" + noticeId));
        return NoticeDto.Detail.from(notice);
    }

    @Transactional
    public Long create(NoticeDto.SaveRequest req, Long adminId) {
        Notice notice = Notice.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .isPinned(req.getIsPinned() != null ? req.getIsPinned() : false)
                .status(req.getStatus() != null ? req.getStatus() : Notice.STATUS_DRAFT)
                .createdBy(adminId)
                .build();
        return noticeRepository.save(notice).getNoticeId();
    }

    @Transactional
    public void update(Long noticeId, NoticeDto.SaveRequest req, Long adminId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. id=" + noticeId));
        notice.update(req.getTitle(), req.getContent(), req.getIsPinned(), req.getStatus(), adminId);
    }

    @Transactional
    public void delete(Long noticeId) {
        noticeRepository.deleteById(noticeId);
    }

    /** 목록에서 '고정' 토글 버튼용 */
    @Transactional
    public void togglePin(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. id=" + noticeId));
        notice.setIsPinned(!Boolean.TRUE.equals(notice.getIsPinned()));
    }
}