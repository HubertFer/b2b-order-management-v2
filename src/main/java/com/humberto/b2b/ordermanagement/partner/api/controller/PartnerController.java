package com.humberto.b2b.ordermanagement.partner.api.controller;

import com.humberto.b2b.ordermanagement.partner.api.dto.CreatePartnerRequest;
import com.humberto.b2b.ordermanagement.partner.api.dto.PartnerResponse;
import com.humberto.b2b.ordermanagement.partner.application.CreatePartnerService;
import com.humberto.b2b.ordermanagement.partner.application.GetPartnerService;
import com.humberto.b2b.ordermanagement.partner.domain.Partner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/partners")
@Tag(name = "Partners", description = "Partner registration and credit balance")
public class PartnerController {

    private final CreatePartnerService createPartnerService;
    private final GetPartnerService getPartnerService;

    PartnerController(CreatePartnerService createPartnerService, GetPartnerService getPartnerService) {
        this.createPartnerService = createPartnerService;
        this.getPartnerService = getPartnerService;
    }

    @PostMapping
    @Operation(summary = "Registers a partner", description = "The partner starts with no used credit.")
    @ApiResponse(responseCode = "201", description = "Partner created")
    @ApiResponse(responseCode = "400", description = "Invalid credit limit",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = createPartnerService.create(request.creditLimit());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(partner.id())
                .toUri();
        return ResponseEntity.created(location).body(PartnerResponse.from(partner));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a partner with its credit balance",
            description = "Returns creditLimit, usedCredit and availableCredit (creditLimit - usedCredit).")
    @ApiResponse(responseCode = "200", description = "Partner found")
    @ApiResponse(responseCode = "404", description = "Partner not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PartnerResponse getById(@Parameter(example = "22222222-2222-2222-2222-222222222222") @PathVariable UUID id) {
        return PartnerResponse.from(getPartnerService.getById(id));
    }
}
