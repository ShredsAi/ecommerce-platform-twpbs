package ai.shreds.adapter.primary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

import ai.shreds.application.ports.ApplicationCreatePaymentIntentInputPort;
import ai.shreds.application.ports.ApplicationConfirmPaymentIntentInputPort;
import ai.shreds.application.ports.ApplicationGetPaymentInputPort;
import ai.shreds.application.dtos.ApplicationCreatePaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationPaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationConfirmPaymentIntentDTO;
import ai.shreds.application.dtos.ApplicationPaymentConfirmationDTO;
import ai.shreds.application.dtos.ApplicationPaymentDetailsDTO;
import ai.shreds.shared.dtos.SharedCreatePaymentIntentRequest;
import ai.shreds.shared.dtos.SharedConfirmPaymentIntentRequest;
import ai.shreds.shared.dtos.SharedPaymentIntentResponse;
import ai.shreds.shared.dtos.SharedPaymentConfirmationResponse;
import ai.shreds.shared.dtos.SharedPaymentDetailsResponse;

@RestController
@RequestMapping("/api")
@Validated
public class AdapterPaymentController {

    private final ApplicationCreatePaymentIntentInputPort applicationCreatePaymentIntentPort;
    private final ApplicationConfirmPaymentIntentInputPort applicationConfirmPaymentIntentPort;
    private final ApplicationGetPaymentInputPort applicationGetPaymentPort;

    public AdapterPaymentController(
            ApplicationCreatePaymentIntentInputPort applicationCreatePaymentIntentPort,
            ApplicationConfirmPaymentIntentInputPort applicationConfirmPaymentIntentPort,
            ApplicationGetPaymentInputPort applicationGetPaymentPort) {
        this.applicationCreatePaymentIntentPort = applicationCreatePaymentIntentPort;
        this.applicationConfirmPaymentIntentPort = applicationConfirmPaymentIntentPort;
        this.applicationGetPaymentPort = applicationGetPaymentPort;
    }

    @PostMapping("/payment-intents")
    public ResponseEntity<SharedPaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody SharedCreatePaymentIntentRequest request) {
        ApplicationCreatePaymentIntentDTO applicationDto = request.toApplicationDTO();
        ApplicationPaymentIntentDTO paymentIntentDto = applicationCreatePaymentIntentPort.createPaymentIntent(applicationDto);
        SharedPaymentIntentResponse response = SharedPaymentIntentResponse.fromApplicationDTO(paymentIntentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payment-intents/{id}/confirm")
    public ResponseEntity<SharedPaymentConfirmationResponse> confirmPaymentIntent(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SharedConfirmPaymentIntentRequest request) {
        ApplicationConfirmPaymentIntentDTO applicationDto = request.toApplicationDTO();
        ApplicationPaymentConfirmationDTO confirmationDto = applicationConfirmPaymentIntentPort.confirmPaymentIntent(id, applicationDto);
        SharedPaymentConfirmationResponse response = SharedPaymentConfirmationResponse.fromApplicationDTO(confirmationDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<SharedPaymentDetailsResponse> getPayment(@PathVariable("id") UUID id) {
        ApplicationPaymentDetailsDTO detailsDto = applicationGetPaymentPort.getPayment(id);
        SharedPaymentDetailsResponse response = SharedPaymentDetailsResponse.fromApplicationDTO(detailsDto);
        return ResponseEntity.ok(response);
    }
}
