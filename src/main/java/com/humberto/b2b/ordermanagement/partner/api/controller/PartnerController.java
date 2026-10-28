package com.humberto.b2b.ordermanagement.partner.api.controller;

import com.humberto.b2b.ordermanagement.partner.api.dto.CreatePartnerRequest;
import com.humberto.b2b.ordermanagement.partner.api.dto.PartnerResponse;
import com.humberto.b2b.ordermanagement.partner.application.CreatePartnerService;
import com.humberto.b2b.ordermanagement.partner.application.GetPartnerService;
import com.humberto.b2b.ordermanagement.partner.domain.Partner;
import jakarta.validation.Valid;
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
public class PartnerController {

    private final CreatePartnerService createPartnerService;
    private final GetPartnerService getPartnerService;

    PartnerController(CreatePartnerService createPartnerService, GetPartnerService getPartnerService) {
        this.createPartnerService = createPartnerService;
        this.getPartnerService = getPartnerService;
    }

    @PostMapping
    ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = createPartnerService.create(request.creditLimit());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(partner.id())
                .toUri();
        return ResponseEntity.created(location).body(PartnerResponse.from(partner));
    }

    @GetMapping("/{id}")
    PartnerResponse getById(@PathVariable UUID id) {
        return PartnerResponse.from(getPartnerService.getById(id));
    }
}
