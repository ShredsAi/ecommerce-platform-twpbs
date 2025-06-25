package ai.shreds.application.ports;

import ai.shreds.domain.value_objects.DomainOrderAggregate;
import java.util.Optional;

public interface ApplicationOrderRepositoryOutputPort {

    DomainOrderAggregate save(DomainOrderAggregate order);
    Optional<DomainOrderAggregate> findByCartId(String cartId);
}
