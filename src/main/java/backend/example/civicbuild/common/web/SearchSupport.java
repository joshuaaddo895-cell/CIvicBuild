package backend.example.civicbuild.common.web;

public final class SearchSupport {

    private SearchSupport() {}

    /** Returns a lowercased LIKE pattern, or null when the query is blank (skip filter). */
    public static String likePattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + query.trim().toLowerCase() + "%";
    }
}
