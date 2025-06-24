package ai.shreds.infrastructure.repositories;

import org.springframework.stereotype.Component;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.domain.entities.DomainAddressEntity;
import ai.shreds.domain.value_objects.DomainOrderAggregate;

import java.util.List;

@Component
public class InfrastructureOrderMapper {

    public DomainOrderAggregate toAggregate(DomainOrderEntity orderEntity,
                                            List<DomainOrderItemEntity> orderItems,
                                            DomainPaymentDetailsEntity paymentDetails,
                                            DomainShippingDetailsEntity shippingDetails,
                                            DomainAddressEntity billingAddress,
                                            DomainAddressEntity shippingAddress) {
        return new DomainOrderAggregate(orderEntity, orderItems, paymentDetails, shippingDetails, billingAddress, shippingAddress);
    }

    public DomainOrderEntity fromAggregate(DomainOrderAggregate aggregate) {
        return aggregate.getOrder();
    }

    public List<DomainOrderItemEntity> mapOrderItems(List<DomainOrderItemEntity> items) {
        return items;
    }

    public DomainPaymentDetailsEntity mapPaymentDetails(DomainPaymentDetailsEntity payment) {
        return payment;
    }

    public DomainShippingDetailsEntity mapShippingDetails(DomainShippingDetailsEntity shipping) {
        return shipping;
    }
}