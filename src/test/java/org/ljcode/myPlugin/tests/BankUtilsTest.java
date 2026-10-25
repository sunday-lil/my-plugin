package org.ljcode.myPlugin.tests;

import org.junit.jupiter.api.Test;
import org.ljcode.myPlugin.utils.BankUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankUtilsTest {

    @Test
    void acceptsPositiveAmounts() {
        assertEquals(10.5, BankUtils.parseAmount("10.5"), 0.000001);
        assertEquals(1.0, BankUtils.parseAmount("1"), 0.000001);
        assertTrue(BankUtils.isValidAmount("0.01"));
    }

    @Test
    void rejectsZeroNegativeNaNInfinityAndGarbage() {
        for (String value : new String[]{"0", "-1", "NaN", "Infinity", "-Infinity", "abc"}) {
            assertFalse(BankUtils.isValidAmount(value), value);
            assertThrows(NumberFormatException.class, () -> BankUtils.parseAmount(value), value);
        }
    }

    @Test
    void nonNegativeAmountAllowsZeroButRejectsNegativeAndNaN() {
        assertEquals(0.0, BankUtils.parseNonNegativeAmount("0"), 0.000001);
        assertEquals(2.5, BankUtils.parseNonNegativeAmount("2.5"), 0.000001);
        assertThrows(NumberFormatException.class, () -> BankUtils.parseNonNegativeAmount("-0.1"));
        assertThrows(NumberFormatException.class, () -> BankUtils.parseNonNegativeAmount("NaN"));
    }
}
