package com.phuzle.labs.messages.domain.text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
    @Test fun sameNumberInDifferentFormats() {
        assertTrue(PhoneNumbers.same("+91 98765 43210", "09876543210"))
        assertTrue(PhoneNumbers.same("9876543210", "+919876543210"))
    }

    @Test fun differentNumbers() = assertFalse(PhoneNumbers.same("9876543210", "9876543211"))

    @Test fun alphanumericSendersCompareAsText() {
        assertTrue(PhoneNumbers.same("HDFCBK", " hdfcbk "))
        assertFalse(PhoneNumbers.same("HDFCBK", "ICICIB"))
    }

    @Test fun shortcodesAreNotFuzzyMatched() = assertFalse(PhoneNumbers.same("56767", "567671"))
}
