package kr.co.firstdayproject.repository.job;

import java.util.List;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.UserDesiredJob;
import kr.co.firstdayproject.entity.job.UserDesiredJobId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDesiredJobRepository extends JpaRepository<UserDesiredJob, UserDesiredJobId> {

    long countByIdJobCategoryId(Long jobCategoryId);

    boolean existsByIdUserId(Long userId);

    void deleteByIdUserId(Long userId);

    @Query("""
            select j
            from UserDesiredJob d
            join JobCategory j on j.jobCategoryId = d.id.jobCategoryId
            where d.id.userId = :userId
            order by d.displayOrder asc
            """)
    List<JobCategory> findJobCategoriesByUserId(@Param("userId") Long userId);
}
