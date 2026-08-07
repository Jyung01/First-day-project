package kr.co.firstdayproject.dto.salary;

import lombok.Data;

@Data
public class SalaryCareerStatDTO {
    private String careerLabel;
    private Long averageSalary;
    private Integer recordCount;
    private Integer barPercent;
}
