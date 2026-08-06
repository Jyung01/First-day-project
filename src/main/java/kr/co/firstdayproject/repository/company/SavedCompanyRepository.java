package kr.co.firstdayproject.repository.company;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.company.SavedCompany;
import kr.co.firstdayproject.entity.company.SavedCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedCompanyRepository extends JpaRepository<SavedCompany, SavedCompanyId> {

    long countByIdUserId(Long userId);

    @Query("""
            select count(jp)
              from SavedCompany s
              join JobPosting jp on jp.companyId = s.id.companyId
             where s.id.userId = :userId
               and jp.status = '모집중'
               and jp.publishedAt is not null
               and jp.publishedAt >= :since
            """)
    long countNewJobPostings(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}
