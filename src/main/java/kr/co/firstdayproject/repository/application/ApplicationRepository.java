package kr.co.firstdayproject.repository.application;

import kr.co.firstdayproject.dto.my.ApplicationSummaryProjection;
import kr.co.firstdayproject.entity.application.Application;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    long countByJobPostingIdAndCurrentStatusNot(
        Long jobPostingId,
        String excludedStatus
    );

    long countByApplicantUserId(Long userId);

    long countByApplicantUserIdAndCurrentStatusIn(
        Long userId,
        Collection<String> statuses
    );

    @Query("""
            select a.applicationId as applicationId,
                   c.companyName as companyName,
                   jp.title as jobTitle,
                   a.currentStatus as currentStatus,
                   a.appliedAt as appliedAt
              from Application a
              join JobPosting jp on jp.jobPostingId = a.jobPostingId
              join Company c on c.companyId = jp.companyId
             where a.applicantUserId = :userId
             order by a.appliedAt desc
            """)
    List<ApplicationSummaryProjection> findRecentByApplicantUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(*)
              FROM applications a
              JOIN job_postings jp
                ON jp.job_posting_id = a.job_posting_id
             WHERE jp.company_id = :companyId
               AND jp.status = '모집중'
               AND a.current_status IN (:statuses)
            """, nativeQuery = true)
    long countActiveApplicantsOfRecruitingCompany(
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<String> statuses
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE applications a
              JOIN job_postings jp
                ON jp.job_posting_id = a.job_posting_id
               SET a.current_status = '채용종료',
                   a.termination_reason = '기업탈퇴',
                   a.terminated_at = :terminatedAt,
                   a.updated_at = :terminatedAt
             WHERE jp.company_id = :companyId
               AND jp.status = '모집중'
               AND a.current_status IN (:statuses)
            """, nativeQuery = true)
    int terminateActiveApplicationsForCompanyWithdrawal(
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<String> statuses,
            @Param("terminatedAt") LocalDateTime terminatedAt
    );
}
