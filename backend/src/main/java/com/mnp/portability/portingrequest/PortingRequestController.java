package com.mnp.portability.portingrequest;

import com.mnp.portability.common.security.CurrentOperator;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.portingrequest.dto.CreatePortingRequest;
import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/porting-requests")
@RequiredArgsConstructor
public class PortingRequestController {

    private final PortingRequestService portingRequestService;

    @Operation(summary = "Submit a porting request",
            description = "The calling operator becomes the recipient; the donor is the number's current holder.")
    @PostMapping
    public ResponseEntity<PortingRequestResponse> submit(@Valid @RequestBody CreatePortingRequest body,
                                                         @CurrentOperator Operator recipient) {
        PortingRequestResponse created = portingRequestService.submit(body, recipient);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}