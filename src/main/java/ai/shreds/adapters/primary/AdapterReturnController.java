package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.shared.dtos.SharedReturnResponseDTO;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.value_objects.SharedReturnRequestParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/returns")
@Validated
public class AdapterReturnController {

    private final ApplicationReturnInputPort returnService;

    @Autowired
    public AdapterReturnController(ApplicationReturnInputPort returnService) {
        this.returnService = returnService;
    }

    @PostMapping
    public ResponseEntity<SharedReturnResponseDTO> requestReturn(
            @Valid @RequestBody SharedReturnRequestParams params) {
        SharedReturnResponseDTO response = returnService.requestReturn(params);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<SharedReturnResponseDTO> getReturn(
            @PathVariable String returnId) {
        SharedReturnResponseDTO response = returnService.getReturn(returnId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<SharedReturnResponseDTO>> getReturnsByOrder(
            @RequestParam String orderId) {
        List<SharedReturnResponseDTO> responses = returnService.getReturnsByOrder(orderId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{returnId}/status")
    public ResponseEntity<SharedReturnResponseDTO> updateReturnStatus(
            @PathVariable String returnId,
            @RequestBody SharedReturnStatusEnum status) {
        SharedReturnResponseDTO response = returnService.updateReturnStatus(returnId, status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}