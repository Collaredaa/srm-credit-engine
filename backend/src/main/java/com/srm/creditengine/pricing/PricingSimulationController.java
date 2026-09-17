package com.srm.creditengine.pricing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing/simulations")
public class PricingSimulationController {

    private final PricingSimulationService pricingSimulationService;

    public PricingSimulationController(PricingSimulationService pricingSimulationService) {
        this.pricingSimulationService = pricingSimulationService;
    }

    @PostMapping
    public ResponseEntity<PricingSimulationResponse> simulate(
            @Valid @RequestBody PricingSimulationRequest request) {
        return ResponseEntity.ok(pricingSimulationService.simulate(request));
    }
}
