package kr.co.firstdayproject.dao.salary;

import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import kr.co.firstdayproject.dto.salary.SalaryCareerStatDTO;
import kr.co.firstdayproject.dto.salary.SalaryJobStatDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SalaryDao {

    List<SalaryRecordsDTO> selectSalaryList(Map<String, Object> search);

    int selectSalaryCompanyCount(Map<String, Object> search);

    SalaryRecordsDTO selectSalarySummary();

    List<String> selectSalaryIndustryList();

    List<String> selectSalaryCompanySizeList();

    SalaryRecordsDTO selectSalaryCompanyDetail(Long companyId);

    List<SalaryCareerStatDTO> selectSalaryCareerStats(Long companyId);

    List<SalaryJobStatDTO> selectSalaryJobStats(Long companyId);

    List<SalaryRecordsDTO> selectSalaryCompanyOptions();

    List<SalaryJobStatDTO> selectSalaryJobCategoryOptions();

    int selectSalaryCompanyExists(Long companyId);

    int selectSalaryJobCategoryExists(Long jobCategoryId);

    int selectSalaryRecordDuplicateCount(@Param("userId") Long userId,
                                         @Param("companyId") Long companyId,
                                         @Param("salaryYear") Integer salaryYear);

    int insertSalaryRecord(SalaryRecordsDTO dto);

    SalaryRecordsDTO selectSalaryRecordForEdit(@Param("userId") Long userId,
                                               @Param("salaryRecordId") Long salaryRecordId);

    int selectSalaryRecordDuplicateExceptSelf(@Param("userId") Long userId,
                                              @Param("companyId") Long companyId,
                                              @Param("salaryYear") Integer salaryYear,
                                              @Param("salaryRecordId") Long salaryRecordId);

    int updateSalaryRecord(SalaryRecordsDTO dto);

    List<SalaryRecordsDTO> selectMySalaryList(@Param("userId") Long userId);

    int deleteSalaryRecord(@Param("userId") Long userId,
                           @Param("salaryRecordId") Long salaryRecordId);
}
