package kr.co.firstdayproject.service.cs;

import jakarta.persistence.EntityNotFoundException;
import kr.co.firstdayproject.dto.cs.FaqDto;
import kr.co.firstdayproject.entity.cs.Faq;
import kr.co.firstdayproject.entity.cs.FaqCategory;
import kr.co.firstdayproject.repository.cs.FaqCategoryRepository;
import kr.co.firstdayproject.repository.cs.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    // 등록/수정 즉시 사용자 화면에 노출되므로 항상 이 상태로 고정 (임시저장·비공개 기능 미사용)
    private static final String STATUS_PUBLIC = "공개";
    private static final int PAGE_SIZE = 10;

    private final FaqRepository faqRepository;
    private final FaqCategoryRepository faqCategoryRepository;

    // 카테고리 목록 (필터·등록 셀렉트박스 공용)
    public List<FaqDto.CategoryOption> getCategories() {
        return faqCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(FaqDto.CategoryOption::from)
                .collect(Collectors.toList());
    }

    // 관리자 대시보드 상단 통계용 - FAQ 전체 건수
    public long getTotalCount() {
        return faqRepository.count();
    }

    // 관리자 목록 (카테고리 필터 + 질문 검색 + 페이지네이션)
    public FaqDto.ListResponse getAdminList(Long faqCategoryId, String keyword, int page) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE);
        boolean hasCategory = faqCategoryId != null;
        boolean hasKeyword = StringUtils.hasText(keyword);

        Page<Faq> result;
        if (hasCategory && hasKeyword) {
            result = faqRepository.findByFaqCategoryIdAndQuestionContainingOrderByCreatedAtDesc(faqCategoryId, keyword, pageable);
        } else if (hasCategory) {
            result = faqRepository.findByFaqCategoryIdOrderByCreatedAtDesc(faqCategoryId, pageable);
        } else if (hasKeyword) {
            result = faqRepository.findByQuestionContainingOrderByCreatedAtDesc(keyword, pageable);
        } else {
            result = faqRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Map<Long, String> categoryNames = categoryNameMap();
        List<FaqDto.ListItem> items = result.getContent().stream()
                .map(f -> FaqDto.ListItem.from(f, categoryNames.get(f.getFaqCategoryId())))
                .collect(Collectors.toList());

        return FaqDto.ListResponse.builder()
                .items(items)
                .totalCount(result.getTotalElements())
                .totalPages(Math.max(result.getTotalPages(), 1))
                .page(page)
                .build();
    }

    // 수정 모달 프리필용 상세 조회
    public FaqDto.Detail getDetail(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. id=" + faqId));
        return FaqDto.Detail.from(faq, categoryNameMap().get(faq.getFaqCategoryId()));
    }

    // 사용자 고객센터 노출용 목록 (공개 상태만, 카테고리 필터 + 키워드 검색)
    public List<FaqDto.ListItem> getPublicList(Long faqCategoryId, String keyword) {
        List<Faq> faqs = (faqCategoryId != null)
                ? faqRepository.findByStatusAndFaqCategoryIdOrderByCreatedAtDesc(STATUS_PUBLIC, faqCategoryId)
                : faqRepository.findByStatusOrderByCreatedAtDesc(STATUS_PUBLIC);

        Map<Long, String> categoryNames = categoryNameMap();

        return faqs.stream()
                .filter(f -> !StringUtils.hasText(keyword)
                        || f.getQuestion().contains(keyword)
                        || f.getAnswer().contains(keyword))
                .map(f -> FaqDto.ListItem.from(f, categoryNames.get(f.getFaqCategoryId())))
                .collect(Collectors.toList());
    }

    // FAQ 등록 -> 즉시 사용자 화면 노출
    @Transactional
    public Long create(FaqDto.SaveRequest request, Long adminId) {
        LocalDateTime now = LocalDateTime.now();
        Faq faq = Faq.builder()
                .faqCategoryId(request.getFaqCategoryId())
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .displayOrder(0)
                .status(STATUS_PUBLIC)
                .createdBy(adminId)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return faqRepository.save(faq).getFaqId();
    }

    // FAQ 수정 -> 수정 즉시 반영
    @Transactional
    public void update(Long faqId, FaqDto.SaveRequest request, Long adminId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. id=" + faqId));
        faq.setFaqCategoryId(request.getFaqCategoryId());
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setUpdatedAt(LocalDateTime.now());
        faq.setUpdatedBy(adminId);
    }

    // FAQ 삭제
    @Transactional
    public void delete(Long faqId) {
        if (!faqRepository.existsById(faqId)) {
            throw new EntityNotFoundException("FAQ를 찾을 수 없습니다. id=" + faqId);
        }
        faqRepository.deleteById(faqId);
    }

    private Map<Long, String> categoryNameMap() {
        return faqCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(FaqCategory::getFaqCategoryId, FaqCategory::getCategoryName));
    }
}