package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobPosting;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    long countByJobCategoryId(Long jobCategoryId);

    long countByCompanyIdAndStatus(
            Long companyId,
            String status
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE JobPosting jobPosting
               SET jobPosting.status = '마감',
                   jobPosting.closeReason = '기업탈퇴',
                   jobPosting.closedAt = :closedAt,
                   jobPosting.updatedAt = :closedAt
             WHERE jobPosting.companyId = :companyId
               AND jobPosting.status = '모집중'
            """)
    int closeRecruitingPostingsForWithdrawal(
            @Param("companyId") Long companyId,
            @Param("closedAt") LocalDateTime closedAt
    );
}
