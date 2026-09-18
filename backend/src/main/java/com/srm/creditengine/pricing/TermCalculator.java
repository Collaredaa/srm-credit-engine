package com.srm.creditengine.pricing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TermCalculator {

    public int calculate(LocalDate currentDate, LocalDate dueDate) {
        Objects.requireNonNull(currentDate, "currentDate must not be null");
        Objects.requireNonNull(dueDate, "dueDate must not be null");

        if (dueDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("dueDate must not be before the current date");
        }

        long termInMonths = ChronoUnit.MONTHS.between(currentDate, dueDate);
        return Math.toIntExact(termInMonths);
    }
}
