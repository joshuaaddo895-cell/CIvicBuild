package backend.example.civicbuild.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationSupport {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private PaginationSupport() {}

    public static Pageable pageable(Integer page, Integer limit) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public static Pageable pageableAsc(Integer page, Integer limit, String sortField) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.ASC, sortField));
    }
}
