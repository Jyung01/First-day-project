package kr.co.firstdayproject.service.my;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.co.firstdayproject.dto.my.CoverLetterDto;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import kr.co.firstdayproject.exception.AccessDeniedException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.coverletter.CoverLetterAiReviewRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterItemRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.service.ai.UserProfileEmbeddingSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterItemRepository coverLetterItemRepository;
    private final CoverLetterAiReviewRepository coverLetterAiReviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<CoverLetterDto.ListItem> findMyList(Long userId) {
        // userId에 해당하고 deletedAt이 null인 데이터만 조회
        List<CoverLetter> letters = coverLetterRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        List<CoverLetterDto.ListItem> result = new ArrayList<>();
        for (CoverLetter letter : letters) {
            List<CoverLetterItem> items =
                    coverLetterItemRepository.findByCoverLetterIdOrderByDisplayOrderAsc(letter.getCoverLetterId());

            int charCount = items.stream()
                    .mapToInt(item -> item.getAnswer() == null ? 0 : item.getAnswer().length())
                    .sum();

            boolean isAiReviewed = coverLetterAiReviewRepository
                    .existsByCoverLetterId(letter.getCoverLetterId());

            result.add(CoverLetterDto.ListItem.builder()
                    .id(letter.getCoverLetterId())
                    .title(letter.getTitle())
                    .questionCount(items.size())
                    .charCount(charCount)
                    .aiReviewed(isAiReviewed)
                    .build());
        }
        return result;
    }

    public CoverLetter getMine(Long coverLetterId, Long userId) {
        CoverLetter letter = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 자기소개서입니다."));

        if (!letter.getUserId().equals(userId) || letter.getDeletedAt() != null) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        return letter;
    }

    @Transactional
    public void delete(Long coverLetterId, Long userId) {
        CoverLetter letter = getMine(coverLetterId, userId);
        letter.setDeletedAt(LocalDateTime.now());
        eventPublisher.publishEvent(new UserProfileEmbeddingSyncEvent(userId));
    }

    /** 자기소개서 문항 목록만 조회 (수정 폼에서 기존 내용 표시할 때 사용) */
    public List<CoverLetterItem> getItems(Long coverLetterId, Long userId) {
        // 소유권 검증까지 같이 수행
        getMine(coverLetterId, userId);
        return coverLetterItemRepository.findByCoverLetterIdOrderByDisplayOrderAsc(coverLetterId);
    }

    /** AI 첨삭 결과 화면의 "이 내용으로 적용"에서 문항 1개의 답변만 갱신할 때 사용 */
    @Transactional
    public void updateItemAnswer(Long coverLetterItemId, Long userId, String answer) {
        CoverLetterItem item = coverLetterItemRepository.findById(coverLetterItemId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 문항입니다."));

        // 소유권 검증 — 이 문항이 속한 자기소개서가 요청한 사용자 것인지 확인
        getMine(item.getCoverLetterId(), userId);

        item.setAnswer(answer);
        item.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void update(Long coverLetterId, Long userId, CoverLetterDto.CreateRequest request) {
        CoverLetter letter = getMine(coverLetterId, userId);
        LocalDateTime now = LocalDateTime.now();

        letter.setTitle(request.getTitle());
        letter.setUpdatedAt(now);

        // 기존 문항은 지우고 새로 저장 (가장 단순하고 안전한 방식)
        List<CoverLetterItem> existingItems =
                coverLetterItemRepository.findByCoverLetterIdOrderByDisplayOrderAsc(coverLetterId);
        coverLetterItemRepository.deleteAll(existingItems);

        if (request.getItems() != null) {
            for (CoverLetterDto.CreateRequest.Item itemDto : request.getItems()) {
                CoverLetterItem item = CoverLetterItem.builder()
                        .coverLetterId(coverLetterId)
                        .question(itemDto.getQuestion())
                        .answer(itemDto.getAnswer())
                        .displayOrder(itemDto.getDisplayOrder())
                        .updatedAt(now)
                        .build();
                coverLetterItemRepository.save(item);
            }
        }
        eventPublisher.publishEvent(new UserProfileEmbeddingSyncEvent(userId));
    }

    @Transactional
    public Long create(Long userId, CoverLetterDto.CreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        CoverLetter letter = CoverLetter.builder()
                .userId(userId)
                .title(request.getTitle())
                .status("사용중")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // deletedAt은 null 상태로 저장됩니다.
        coverLetterRepository.save(letter);

        if (request.getItems() != null) {
            for (CoverLetterDto.CreateRequest.Item itemDto : request.getItems()) {
                CoverLetterItem item = CoverLetterItem.builder()
                        .coverLetterId(letter.getCoverLetterId())
                        .question(itemDto.getQuestion())
                        .answer(itemDto.getAnswer())
                        .displayOrder(itemDto.getDisplayOrder())
                        .updatedAt(now)
                        .build();
                coverLetterItemRepository.save(item);
            }
        }
        eventPublisher.publishEvent(new UserProfileEmbeddingSyncEvent(userId));

        return letter.getCoverLetterId();
    }
}
