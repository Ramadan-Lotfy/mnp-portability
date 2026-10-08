package com.mnp.portability.portingrequest;

import com.mnp.portability.common.security.CurrentOperator;
import com.mnp.portability.operator.Operator;
import com.mnp.portability.portingrequest.dto.CreatePortingRequest;
import com.mnp.portability.portingrequest.dto.PortingRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "List porting requests",
            description = "Donors and recipients see their requests in any status; other operators see "
                    + "accepted requests only. Newest first; the sort cannot be changed.")
    @GetMapping
    public PagedModel<PortingRequestResponse> list(
            @Parameter(description = "Only requests in this status") @RequestParam(required = false) PortingStatus status,
            @Parameter(hidden = true) Pageable pageable,
            @CurrentOperator Operator caller) {
        return new PagedModel<>(portingRequestService.list(caller, status, pageable));
    }

    @Operation(summary = "Get one porting request",
            description = "404 when the request does not exist or the caller is not allowed to see it.")
    @GetMapping("/{id}")
    public PortingRequestResponse get(@PathVariable Long id, @CurrentOperator Operator caller) {
        return portingRequestService.get(id, caller);
    }

    @Operation(summary = "Accept a porting request", description = "Only the donor operator can accept.")
    @PostMapping("/{id}/accept")
    public PortingRequestResponse accept(@PathVariable Long id, @CurrentOperator Operator donor) {
        return portingRequestService.accept(id, donor);
    }

    @Operation(summary = "Reject a porting request", description = "Only the donor operator can reject.")
    @PostMapping("/{id}/reject")
    public PortingRequestResponse reject(@PathVariable Long id, @CurrentOperator Operator donor) {
        return portingRequestService.reject(id, donor);
    }
}