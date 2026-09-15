package com.srm.creditengine.pricing;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ChequePricingStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.025");

    @Override
    public ReceivableType supportedType() {
        return ReceivableType.CHEQUE_PRE_DATADO;
    }

    @Override
    public BigDecimal spread() {
        return SPREAD;
    }
}
