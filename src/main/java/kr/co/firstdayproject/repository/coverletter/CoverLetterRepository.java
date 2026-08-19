package kr.co.firstdayproject.repository.coverletter;

import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {

    List<CoverLetter> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<CoverLetter> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    Optional<CoverLetter> findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * 회원 탈퇴 시 자기소개서를 실제로 삭제한다(소프트 삭제 아님).
     * 문항과 AI 첨삭 결과는 FK ON DELETE CASCADE로 함께 지워지고,
     * applications.cover_letter_id는 ON DELETE SET NULL이라 지원 이력은 유지된다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CoverLetter c where c.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
