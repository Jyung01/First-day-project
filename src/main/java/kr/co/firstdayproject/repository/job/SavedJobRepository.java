package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.SavedJob;
import kr.co.firstdayproject.entity.job.SavedJobId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {
}
