package kr.co.firstdayproject.dto.common;

import org.springframework.data.domain.Page;

/**
 * 화면 페이지네이션 정보.
 *
 * Spring Data의 페이지 번호는 0부터 시작한다.
 * 기본적으로 페이지 번호를 5개 단위로 출력한다.
 */
public record PageInfo(
        int currentPage,
        int startPage,
        int endPage,
        int totalPages,
        boolean hasPreviousBlock,
        boolean hasNextBlock,
        int previousBlockPage,
        int nextBlockPage
) {

    private static final int DEFAULT_BLOCK_SIZE = 5;

    /**
     * 기본 5페이지 단위로 페이지 정보를 생성한다.
     */
    public static PageInfo of(Page<?> page) {
        return of(page, DEFAULT_BLOCK_SIZE);
    }

    /**
     * 지정한 페이지 블록 크기로 페이지 정보를 생성한다.
     */
    public static PageInfo of(
            Page<?> page,
            int blockSize
    ) {
        if (page == null) {
            throw new IllegalArgumentException(
                    "페이지 정보는 null일 수 없습니다."
            );
        }

        if (blockSize < 1) {
            throw new IllegalArgumentException(
                    "페이지 블록 크기는 1 이상이어야 합니다."
            );
        }

        int currentPage = page.getNumber();
        int totalPages = page.getTotalPages();

        /*
         * 검색 결과가 0건이면 totalPages가 0이다.
         * 화면에서 잘못된 페이지 범위가 만들어지지 않도록
         * 시작과 종료 페이지를 모두 0으로 둔다.
         */
        if (totalPages == 0) {
            return new PageInfo(
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    0,
                    0
            );
        }

        int startPage =
                (currentPage / blockSize) * blockSize;

        int endPage = Math.min(
                startPage + blockSize - 1,
                totalPages - 1
        );

        boolean hasPreviousBlock = startPage > 0;
        boolean hasNextBlock = endPage < totalPages - 1;

        int previousBlockPage = Math.max(
                startPage - 1,
                0
        );

        int nextBlockPage = Math.min(
                endPage + 1,
                totalPages - 1
        );

        return new PageInfo(
                currentPage,
                startPage,
                endPage,
                totalPages,
                hasPreviousBlock,
                hasNextBlock,
                previousBlockPage,
                nextBlockPage
        );
    }
}