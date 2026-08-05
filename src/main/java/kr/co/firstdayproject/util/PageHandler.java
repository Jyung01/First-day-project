package kr.co.firstdayproject.util;

import lombok.Getter;

@Getter
public class PageHandler {

    private final int currentPage;
    private final int pageSize;
    private final int total;

    private final int lastPage;
    private final int startPage;
    private final int endPage;

    public PageHandler(int currentPage, int total, int pageSize) {

        this.currentPage = currentPage;
        this.total = total;
        this.pageSize = pageSize;

        // 마지막 페이지
        this.lastPage = (int) Math.ceil((double) total / pageSize);

        // 페이지 네비게이션(10개씩)
        this.startPage = ((currentPage - 1) / 10) * 10 + 1;

        int tempEndPage = startPage + 9;
        this.endPage = Math.min(tempEndPage, lastPage);
    }

    // LIMIT의 시작 위치 ( 현재 페이지에서 몇 번째 데이터부터 가져와야 하는지 계산)
    public int getOffset() {
        return (currentPage - 1) * pageSize;
    }
}