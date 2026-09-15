package com.srm.creditengine.pricing;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DuplicataPricingStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.015");

    @Override
    public ReceivableType supportedType() {
        return ReceivableType.DUPLICATA_MERCANTIL;
    }

    @Override
    public BigDecimal spread() {
        return SPREAD;
    }
}
