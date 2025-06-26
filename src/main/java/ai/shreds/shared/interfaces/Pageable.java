package ai.shreds.shared.interfaces;

import org.springframework.data.domain.Sort;

public interface Pageable {

    Integer getPageNumber();

    Integer getPageSize();

    Sort getSort();
}