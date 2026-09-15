package com.srm.creditengine.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {

    ReceivableType supportedType();

    BigDecimal spread();
}
