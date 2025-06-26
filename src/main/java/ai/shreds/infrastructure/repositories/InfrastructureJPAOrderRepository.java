package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJPAOrderRepository extends JpaRepository<InfrastructureOrderJPAEntity, UUID> {
    List<InfrastructureOrderJPAEntity> findByOrderStatusIn(List<String> statuses);
}
