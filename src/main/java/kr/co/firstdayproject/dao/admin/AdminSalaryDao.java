package kr.co.firstdayproject.dao.admin;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.salary.SalaryJobStatDTO;
import kr.co.firstdayproject.dto.salary.SalaryRecordsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminSalaryDao {
    List<SalaryRecordsDTO> selectAdminSalaryList(Map<String, Object> search);
    int selectAdminSalaryCount(Map<String, Object> search);
    Map<String, Object> selectAdminSalarySummary();
    SalaryRecordsDTO selectAdminSalaryDetail(Long salaryRecordId);
    List<SalaryRecordsDTO> selectCompanyOptions();
    List<SalaryJobStatDTO> selectJobOptions();
    int updateSalaryStatus(@Param("salaryRecordId") Long salaryRecordId,
                           @Param("status") String status,
                           @Param("hiddenReason") String hiddenReason,
                           @Param("adminId") Long adminId);
}
