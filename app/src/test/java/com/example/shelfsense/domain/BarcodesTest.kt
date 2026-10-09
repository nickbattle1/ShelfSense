package com.example.shelfsense.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodesTest {

    @Test
    fun acceptsValidEan13() {
        assertTrue(Barcodes.hasValidCheckDigit("9300675024235"))
        assertNull(Barcodes.problemWith("9300675024235"))
    }

    @Test
    fun rejectsWrongCheckDigit() {
        assertFalse(Barcodes.hasValidCheckDigit("9300675024236"))
        assertNotNull(Barcodes.problemWith("9300675024236"))
    }

    @Test
    fun acceptsValidUpcA() {
        assertTrue(Barcodes.hasValidCheckDigit("036000291452"))
    }

    @Test
    fun letsEightDigitCodesThrough() {
        assertTrue(Barcodes.hasValidCheckDigit("12345670"))
    }

    @Test
    fun rejectsUnusualLengths() {
        assertEquals("Barcodes are usually 8, 12 or 13 digits long", Barcodes.problemWith("12345"))
    }

    @Test
    fun cleanStripsSpacesAndDashes() {
        assertEquals("9300675024235", Barcodes.clean(" 930-0675 024235 "))
    }

    @Test
    fun emptyInputAsksForTheNumber() {
        assertNotNull(Barcodes.problemWith(""))
    }
}
