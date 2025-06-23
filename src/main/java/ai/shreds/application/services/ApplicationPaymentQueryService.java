package ai.shreds.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import ai.shreds.application.dtos.ApplicationPaymentDetailsDTO;
import ai.shreds.application.exceptions.ApplicationPaymentNotFoundException;
import ai.shreds.application.ports.ApplicationGetPaymentInputPort;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.entities.DomainPaymentEntity;

/**
 * Application service for querying payment details
 */
@Service
public class ApplicationPaymentQueryService implements ApplicationGetPaymentInputPort {

    private final DomainOutputPortPaymentRepository paymentRepository;

    public ApplicationPaymentQueryService(DomainOutputPortPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationPaymentDetailsDTO getPayment(UUID id) {
        DomainPaymentEntity payment = paymentRepository.findPaymentById(new DomainPaymentIdValue(id));
        if (payment == null) {
            throw new ApplicationPaymentNotFoundException(id);
        }
        return ApplicationPaymentDetailsDTO.fromDomainEntity(payment);
    }
}