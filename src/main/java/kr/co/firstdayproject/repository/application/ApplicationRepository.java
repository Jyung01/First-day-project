package kr.co.firstdayproject.repository.application;

import kr.co.firstdayproject.entity.application.Application;
import java.util.Collection;
import java.time.LocalDateTime;
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
