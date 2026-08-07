package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyReviewDao;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyReviewService {
    private final CompanyReviewDao companyReviewDao;

    public CompanyReviewsDTO getReviewWriteForm(Long userId, Long companyId) {
        CompanyReviewsDTO form = companyReviewDao.selectReviewEligibility(userId, companyId);
        if (form == null) {
            throw new IllegalStateException("입사 완료 이력이 없거나 이미 기업리뷰를 작성했습니다.");
        }
        form.setCareerGrowthRating(5.0);
        form.setWorkSatisfactionRating(5.0);
        form.setCompensationRating(5.0);
        form.setCultureRating(5.0);
        return form;
    }


    // 기업 상세 - 기업리뷰 : 리뷰요약
    public CompanyReviewsDTO getCompanyReviewSummary(Long companyId) {
        return companyReviewDao.selectCompanyReviewSummary(companyId);
    }

    // 기업 상세 - 기업리뷰 : 리뷰목록
    public List<CompanyReviewsDTO> getCompanyReviewList(
            Long companyId,
            int offset,
            int pageSize, String sort){

        return companyReviewDao.selectCompanyReviewList(companyId, offset, pageSize, sort);
    }

    public int getCompanyReviewCount(Long companyId){

        return companyReviewDao.selectCompanyReviewCount(companyId);
    }

    // 면접 후기 요약
    public InterviewReviewsDTO getInterviewReviewSummary(Long companyId) {
        return companyReviewDao.selectInterviewReviewSummary(companyId);
    }

    // 면접 후기 목록
    public List<InterviewReviewsDTO> getInterviewReviewList(Long companyId,
                                                           int offset,
                                                           int pageSize,
                                                           String sort) {

        return companyReviewDao.selectInterviewReviewList(
                companyId,
                offset,
                pageSize,
                sort
        );
    }

    // 면접 후기 총 개수
    public int getInterviewReviewCount(Long companyId) {
        return companyReviewDao.selectInterviewReviewCount(companyId);
    }

    public InterviewReviewsDTO getInterviewReviewWriteForm(Long userId, Long companyId) {
        InterviewReviewsDTO form = companyReviewDao.selectInterviewReviewEligibility(userId, companyId);
        if (form == null) {
            throw new IllegalStateException("면접 완료 이력이 없거나 이미 면접후기를 작성했습니다.");
        }
        form.setInterviewType("대면면접");
        form.setDifficulty("보통");
        return form;
    }

    @Transactional
    public void insertInterviewReview(Long userId, InterviewReviewsDTO dto) {
        InterviewReviewsDTO eligibility = getInterviewReviewWriteForm(userId, dto.getCompanyId());

        dto.setAuthorUserId(userId);
        dto.setApplicationId(eligibility.getApplicationId());
        dto.setJobPostingId(eligibility.getJobPostingId());
        dto.setInterviewMonth(eligibility.getInterviewMonth());
        dto.setInterviewResult(eligibility.getInterviewResult());
        dto.setProcessText(eligibility.getProcessText());

        validateInterviewReview(dto);

        if (companyReviewDao.insertInterviewReview(dto) != 1) {
            throw new IllegalStateException("면접후기 등록에 실패했습니다.");
        }
    }

    private void validateInterviewReview(InterviewReviewsDTO dto) {
        if (!List.of("대면면접", "화상면접", "전화면접", "기타").contains(dto.getInterviewType())) {
            throw new IllegalArgumentException("올바른 면접 방식을 선택해주세요.");
        }
        if (!List.of("쉬움", "보통", "어려움").contains(dto.getDifficulty())) {
            throw new IllegalArgumentException("올바른 면접 난이도를 선택해주세요.");
        }
        if (isBlank(dto.getContent())) {
            throw new IllegalArgumentException("면접 내용을 입력해주세요.");
        }
    }

    // 기업 리뷰 등록
    @Transactional
    public void insertCompanyReview(Long userId, CompanyReviewsDTO dto) {

        CompanyReviewsDTO eligibility = getReviewWriteForm(userId, dto.getCompanyId());
        dto.setAuthorUserId(userId);
        dto.setEligibilityApplicationId(eligibility.getEligibilityApplicationId());
        dto.setJobCategoryId(eligibility.getJobCategoryId());
        dto.setEmploymentStatus(eligibility.getEmploymentStatus());

        applyDefaultRatings(dto);
        validateReview(dto);

        double avg =
                (dto.getCareerGrowthRating()
                        + dto.getWorkSatisfactionRating()
                        + dto.getCompensationRating()
                        + dto.getCultureRating()) / 4.0;

        dto.setOverallRating(
                BigDecimal.valueOf(avg)
                        .setScale(1, RoundingMode.HALF_UP)
        );

        if (companyReviewDao.insertCompanyReview(dto) != 1) {
            throw new IllegalStateException("기업리뷰 등록에 실패했습니다.");
        }
    }

    private void validateReview(CompanyReviewsDTO dto) {
        validateRating(dto.getCareerGrowthRating());
        validateRating(dto.getWorkSatisfactionRating());
        validateRating(dto.getCompensationRating());
        validateRating(dto.getCultureRating());

        if (isBlank(dto.getPros()) || isBlank(dto.getCons()) || isBlank(dto.getSummary())) {
            throw new IllegalArgumentException("장점, 단점, 한 줄 요약을 모두 입력해주세요.");
        }
        if (dto.getSummary().trim().length() > 300) {
            throw new IllegalArgumentException("한 줄 요약은 300자 이하로 입력해주세요.");
        }
    }

    private void applyDefaultRatings(CompanyReviewsDTO dto) {
        if (dto.getCareerGrowthRating() == null) dto.setCareerGrowthRating(5.0);
        if (dto.getWorkSatisfactionRating() == null) dto.setWorkSatisfactionRating(5.0);
        if (dto.getCompensationRating() == null) dto.setCompensationRating(5.0);
        if (dto.getCultureRating() == null) dto.setCultureRating(5.0);
    }

    private void validateRating(Double rating) {
        if (rating == null || rating < 1 || rating > 5 || rating % 1 != 0) {
            throw new IllegalArgumentException("평점은 1점부터 5점 사이의 정수여야 합니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Transactional
    public Map<String, Object> toggleReviewReaction(Long userId, String reviewType, Long reviewId) {
        if (!List.of("기업리뷰", "면접후기").contains(reviewType)) {
            throw new IllegalArgumentException("올바르지 않은 리뷰 유형입니다.");
        }
        if (reviewId == null || companyReviewDao.selectReviewTargetCount(reviewType, reviewId) == 0) {
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
        }

        boolean helpful;
        if (companyReviewDao.selectUserReviewReactionCount(userId, reviewType, reviewId) > 0) {
            companyReviewDao.deleteReviewReaction(userId, reviewType, reviewId);
            helpful = false;
        } else {
            companyReviewDao.insertReviewReaction(userId, reviewType, reviewId);
            helpful = true;
        }

        int helpCount = companyReviewDao.selectReviewReactionCount(reviewType, reviewId);
        return Map.of("helpful", helpful, "helpCount", helpCount);
    }
}
