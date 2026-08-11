package kr.co.firstdayproject.service.salary;

import kr.co.firstdayproject.dao.salary.SalaryDao;
import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import kr.co.firstdayproject.dto.salary.SalaryCareerStatDTO;
import kr.co.firstdayproject.dto.salary.SalaryJobStatDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryService {

    private final SalaryDao salaryDao;

    public List<SalaryRecordsDTO> getSalaryList(Map<String, Object> search) {
        return salaryDao.selectSalaryList(search);
    }

    public int getSalaryCompanyCount(Map<String, Object> search) {
        return salaryDao.selectSalaryCompanyCount(search);
    }

    public SalaryRecordsDTO getSalarySummary() {
        return salaryDao.selectSalarySummary();
    }

    public List<String> getSalaryIndustryList() {
        return salaryDao.selectSalaryIndustryList();
    }

    public List<String> getSalaryCompanySizeList() {
        return salaryDao.selectSalaryCompanySizeList();
    }

    public SalaryRecordsDTO getSalaryCompanyDetail(Long companyId) {
        return salaryDao.selectSalaryCompanyDetail(companyId);
    }

    public List<SalaryCareerStatDTO> getSalaryCareerStats(Long companyId) {
        List<SalaryCareerStatDTO> stats = salaryDao.selectSalaryCareerStats(companyId);
        long maxSalary = stats.stream()
                .map(SalaryCareerStatDTO::getAverageSalary)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        stats.forEach(stat -> stat.setBarPercent(maxSalary == 0
                ? 0
                : (int) Math.round(stat.getAverageSalary() * 100.0 / maxSalary)));
        return stats;
    }

    public List<SalaryJobStatDTO> getSalaryJobStats(Long companyId) {
        return salaryDao.selectSalaryJobStats(companyId);
    }

    public List<SalaryRecordsDTO> getSalaryCompanyOptions() {
        return salaryDao.selectSalaryCompanyOptions();
    }

    public List<SalaryJobStatDTO> getSalaryJobCategoryOptions() {
        return salaryDao.selectSalaryJobCategoryOptions();
    }

    @Transactional
    public void createSalaryRecord(Long userId, SalaryRecordsDTO dto, boolean agreed) {
        validateSalaryRecord(dto, agreed);
        if (salaryDao.selectSalaryRecordDuplicateCount(
                userId, dto.getCompanyId(), dto.getSalaryYear()) > 0) {
            throw new IllegalStateException("해당 기업의 같은 연도 연봉정보가 이미 등록되어 있습니다.");
        }

        dto.setAuthorUserId(userId);
        if (salaryDao.insertSalaryRecord(dto) != 1) {
            throw new IllegalStateException("연봉정보 등록에 실패했습니다.");
        }
    }

    public SalaryRecordsDTO getSalaryRecordForEdit(Long userId, Long salaryRecordId) {
        SalaryRecordsDTO record = salaryDao.selectSalaryRecordForEdit(userId, salaryRecordId);
        if (record == null) {
            throw new IllegalStateException("수정할 연봉정보를 찾을 수 없습니다.");
        }
        return record;
    }

    @Transactional
    public void updateSalaryRecord(Long userId, SalaryRecordsDTO dto, boolean agreed) {
        getSalaryRecordForEdit(userId, dto.getSalaryRecordId());
        validateSalaryRecord(dto, agreed);
        if (salaryDao.selectSalaryRecordDuplicateExceptSelf(
                userId, dto.getCompanyId(), dto.getSalaryYear(), dto.getSalaryRecordId()) > 0) {
            throw new IllegalStateException("해당 기업의 같은 연도 연봉정보가 이미 등록되어 있습니다.");
        }
        dto.setAuthorUserId(userId);
        if (salaryDao.updateSalaryRecord(dto) != 1) {
            throw new IllegalStateException("연봉정보 수정에 실패했습니다.");
        }
    }

    public List<SalaryRecordsDTO> getMySalaryList(Long userId) {
        return salaryDao.selectMySalaryList(userId);
    }

    @Transactional
    public void deleteSalaryRecord(Long userId, Long salaryRecordId) {
        if (salaryRecordId == null || salaryDao.deleteSalaryRecord(userId, salaryRecordId) != 1) {
            throw new IllegalStateException("삭제할 연봉정보를 찾을 수 없습니다.");
        }
    }

    private void validateSalaryRecord(SalaryRecordsDTO dto, boolean agreed) {
        if (!agreed) {
            throw new IllegalArgumentException("익명 통계 활용에 동의해주세요.");
        }
        if (dto.getCompanyId() == null || salaryDao.selectSalaryCompanyExists(dto.getCompanyId()) == 0) {
            throw new IllegalArgumentException("올바른 기업을 선택해주세요.");
        }
        if (dto.getJobCategoryId() == null
                || salaryDao.selectSalaryJobCategoryExists(dto.getJobCategoryId()) == 0) {
            throw new IllegalArgumentException("올바른 직무를 선택해주세요.");
        }
        if (!List.of("현직원", "전직원").contains(dto.getEmploymentStatus())) {
            throw new IllegalArgumentException("재직 상태를 선택해주세요.");
        }
        if (!List.of("정규직", "계약직", "인턴", "프리랜서", "파견직", "기타")
                .contains(dto.getEmploymentType())) {
            throw new IllegalArgumentException("고용 형태를 선택해주세요.");
        }
        if (dto.getCareerYears() == null || dto.getCareerYears() < 0 || dto.getCareerYears() > 50) {
            throw new IllegalArgumentException("총 경력은 0년부터 50년 사이로 입력해주세요.");
        }
        int currentYear = java.time.Year.now().getValue();
        if (dto.getSalaryYear() == null
                || dto.getSalaryYear() < currentYear - 10
                || dto.getSalaryYear() > currentYear) {
            throw new IllegalArgumentException("연봉 기준연도를 확인해주세요.");
        }
        if (dto.getBaseSalary() == null || dto.getBaseSalary() <= 0
                || dto.getBaseSalary() > 1_000_000) {
            throw new IllegalArgumentException("세전 연봉을 입력해주세요.");
        }
        if (dto.getBonusAmount() != null
                && (dto.getBonusAmount() < 0 || dto.getBonusAmount() > 1_000_000)) {
            throw new IllegalArgumentException("성과급은 0 이상의 금액으로 입력해주세요.");
        }
    }
}
