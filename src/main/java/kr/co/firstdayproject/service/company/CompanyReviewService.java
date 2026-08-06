package kr.co.firstdayproject.service.company;

import kr.co.firstdayproject.dao.company.CompanyDao;
import kr.co.firstdayproject.dao.company.CompanyReviewDao;
import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            int pageSize){

        return companyReviewDao.selectCompanyReviewList(companyId, offset, pageSize);
    }

    public int getCompanyReviewCount(Long companyId){

        return companyReviewDao.selectCompanyReviewCount(companyId);
    }
}
