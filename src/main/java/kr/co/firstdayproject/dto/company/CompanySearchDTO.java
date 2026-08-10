package kr.co.firstdayproject.dto.company;

import lombok.Data;

@Data
public class CompanySearchDTO {

    private String keyword;
    private String industry;
    private String region;
    private String companySize;
    private String jobCategory;

    private int offset;
    private int pageSize;

    private Long userId;
}