package backend.example.civicbuild.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int limit,
        long total,
        boolean hasNextPage) {

    public static <T> PageResponse<T> of(List<T> items, int page, int limit, long total) {
        long offset = (long) page * limit;
        boolean hasNext = offset + items.size() < total;
        return new PageResponse<>(items, page, limit, total, hasNext);
    }
}
