package kr.co.firstdayproject.dto.salary;

import lombok.Data;

@Data
public class SalaryJobStatDTO {
    private Long jobCategoryId;
    private String categoryName;
    private Long averageSalary;
    private Long newcomerAverageSalary;
    private Integer recordCount;
}
