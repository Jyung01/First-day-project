package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.UserDesiredJob;
import kr.co.firstdayproject.entity.job.UserDesiredJobId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDesiredJobRepository extends JpaRepository<UserDesiredJob, UserDesiredJobId> {
}
