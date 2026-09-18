package com.srm.creditengine.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TermCalculatorTest {

    private final TermCalculator termCalculator = new TermCalculator();

    @Test
    void shouldFailWhenDueDateIsYesterday() {
        LocalDate currentDate = LocalDate.of(2026, 9, 18);
        LocalDate dueDate = currentDate.minusDays(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> termCalculator.calculate(currentDate, dueDate));

        assertEquals("dueDate must not be before the current date", exception.getMessage());
    }

    @Test
    void shouldFailWhenDueDateIsEarlierInSameMonth() {
        LocalDate currentDate = LocalDate.of(2026, 9, 18);
        LocalDate dueDate = LocalDate.of(2026, 9, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> termCalculator.calculate(currentDate, dueDate));

        assertEquals("dueDate must not be before the current date", exception.getMessage());
    }

    @Test
    void shouldReturnZeroWhenDueDateIsToday() {
        LocalDate currentDate = LocalDate.of(2026, 9, 18);

        int termInMonths = termCalculator.calculate(currentDate, currentDate);

        assertEquals(0, termInMonths);
    }

    @Test
    void shouldReturnZeroWhenDueDateIsFutureInSameMonth() {
        LocalDate currentDate = LocalDate.of(2026, 9, 18);
        LocalDate dueDate = LocalDate.of(2026, 9, 30);

        int termInMonths = termCalculator.calculate(currentDate, dueDate);

        assertEquals(0, termInMonths);
    }

    @Test
    void shouldReturnThreeWhenDueDateIsThreeMonthsAhead() {
        LocalDate currentDate = LocalDate.of(2026, 9, 18);
        LocalDate dueDate = currentDate.plusMonths(3);

        int termInMonths = termCalculator.calculate(currentDate, dueDate);

        assertEquals(3, termInMonths);
    }
}
