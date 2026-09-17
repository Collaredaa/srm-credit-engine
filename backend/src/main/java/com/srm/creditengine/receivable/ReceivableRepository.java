package com.srm.creditengine.receivable;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {
}
