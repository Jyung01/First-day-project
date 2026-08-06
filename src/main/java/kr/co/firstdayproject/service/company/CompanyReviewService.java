package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyDao;
import kr.co.firstdayproject.dao.company.CompanyReviewDao;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyReviewService {
    private final CompanyReviewDao companyReviewDao;


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

    // 기업 리뷰 등록
    public void insertCompanyReview(CompanyReviewsDTO dto) {

        double avg =
                (dto.getCareerGrowthRating()
                        + dto.getWorkSatisfactionRating()
                        + dto.getCompensationRating()
                        + dto.getCultureRating()) / 4.0;

        dto.setOverallRating(
                BigDecimal.valueOf(avg)
                        .setScale(1, RoundingMode.HALF_UP)
        );

        companyReviewDao.insertCompanyReview(dto);
    }
}
