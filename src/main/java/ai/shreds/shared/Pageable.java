package ai.shreds.shared;

/**
 * Interface for pagination parameters shared across services.
 * This is a simplified version of Spring's Pageable for cross-layer compatibility.
 */
public interface Pageable {

    /**
     * Returns the page to be returned (0-indexed).
     *
     * @return the page number
     */
    Integer getPageNumber();

    /**
     * Returns the number of items to be returned per page.
     *
     * @return the page size
     */
    Integer getPageSize();

    /**
     * Returns the sorting parameters.
     *
     * @return the Sort object
     */
    Sort getSort();

    /**
     * Simple Sort interface for sorting parameters.
     */
    interface Sort {
        /**
         * Returns whether sorting is requested.
         *
         * @return true if sorting is requested
         */
        boolean isSorted();

        /**
         * Returns the direction of sorting.
         *
         * @return the direction (ASC or DESC)
         */
        String getDirection();

        /**
         * Returns the properties to sort by.
         *
         * @return array of property names
         */
        String[] getProperties();
    }
}