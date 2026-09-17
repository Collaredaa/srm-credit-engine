package com.srm.creditengine.currency;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, UUID> {

    Optional<ExchangeRateEntity> findFirstBySourceCurrencyAndTargetCurrencyAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
            Currency sourceCurrency,
            Currency targetCurrency,
            OffsetDateTime effectiveAt);
}
