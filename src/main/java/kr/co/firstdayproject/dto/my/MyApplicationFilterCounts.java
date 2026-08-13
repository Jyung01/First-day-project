package kr.co.firstdayproject.dto.my;

public record MyApplicationFilterCounts(
        long all,
        long applied,
        long progress,
        long result
) {
}
