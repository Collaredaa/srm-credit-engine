package com.srm.creditengine.receivable;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receivables")
public class ReceivableController {

    private final ReceivableRepository receivableRepository;

    public ReceivableController(ReceivableRepository receivableRepository) {
        this.receivableRepository = receivableRepository;
    }

    @PostMapping
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody CreateReceivableRequest request) {
        Receivable receivable = new Receivable(
                request.assignor(),
                request.type(),
                request.faceValue(),
                request.dueDate());

        Receivable savedReceivable = receivableRepository.save(receivable);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReceivableResponse.from(savedReceivable));
    }
}
