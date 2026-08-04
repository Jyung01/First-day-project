package kr.co.firstdayproject.dao.job;

import kr.co.firstdayproject.dto.job.JobDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface JobDao {
    List<JobDTO> selectJobPostingList();
}
