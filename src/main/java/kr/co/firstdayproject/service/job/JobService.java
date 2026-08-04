package kr.co.firstdayproject.service.job;

import kr.co.firstdayproject.dao.job.JobDao;
import kr.co.firstdayproject.dto.job.JobDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {
    private final JobDao jobDao;

    public List<JobDTO> getJobPostingList() {
        return jobDao.selectJobPostingList();
    }
}
