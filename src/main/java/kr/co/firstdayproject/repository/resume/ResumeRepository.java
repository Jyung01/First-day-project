package kr.co.firstdayproject.repository.resume;

import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    List<Resume> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);
}
