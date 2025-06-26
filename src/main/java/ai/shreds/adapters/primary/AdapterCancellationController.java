package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.value_objects.SharedCancellationRequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/cancellations")
@Validated
public class AdapterCancellationController {

    private final ApplicationCancellationInputPort cancellationService;

    @Autowired
    public AdapterCancellationController(ApplicationCancellationInputPort cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping
    public ResponseEntity<SharedCancellationResponseDTO> requestCancellation(
            @Valid @RequestBody SharedCancellationRequestParams params) {
        SharedCancellationResponseDTO response = cancellationService.requestCancellation(params);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{cancellationId}")
    public ResponseEntity<SharedCancellationResponseDTO> getCancellation(
            @PathVariable String cancellationId) {
        SharedCancellationResponseDTO response = cancellationService.getCancellation(cancellationId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<SharedCancellationResponseDTO>> getCancellationsByOrder(
            @RequestParam String orderId) {
        List<SharedCancellationResponseDTO> responses = cancellationService.getCancellationsByOrder(orderId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}