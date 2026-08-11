package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 사용자/관리자 공통 목록 조회
     * - status 가 null 이면 전체(관리자용), 값 있으면 해당 상태만(사용자: '공개')
     * - keyword 가 null/blank 면 제목 검색 무시
     * - 고정글(isPinned=true) 우선, 그 다음 최신순
     */
    @Query("""
            select n from Notice n
            where (:status is null or n.status = :status)
              and (:keyword is null or :keyword = '' or n.title like concat('%', :keyword, '%'))
            order by n.isPinned desc, n.createdAt desc
            """)
    Page<Notice> search(
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}