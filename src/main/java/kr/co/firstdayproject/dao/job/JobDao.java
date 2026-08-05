package kr.co.firstdayproject.dao.job;

import kr.co.firstdayproject.dto.job.JobDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface JobDao {
    // 메인 : 채용 공고 인기순
    List<JobDTO> selectLatestJobPostingList();

    // 메인 : 채용 공고 인기순
    List<JobDTO> selectPopularJobPostingList();
}
