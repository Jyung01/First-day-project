package kr.co.firstdayproject.repository.report;

import kr.co.firstdayproject.entity.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterUserIdAndTargetTypeAndTargetId(
            Long reporterUserId,
            String targetType,
            Long targetId
    );
}
