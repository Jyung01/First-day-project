package kr.co.firstdayproject.repository.resume;

import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    List<Resume> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 회원 탈퇴 시 이력서를 실제로 삭제한다(소프트 삭제 아님).
     * 학력·경력·프로젝트·기술은 FK ON DELETE CASCADE로 함께 지워지고,
     * applications.resume_id는 ON DELETE SET NULL이라 지원 이력은 유지된다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Resume r where r.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
