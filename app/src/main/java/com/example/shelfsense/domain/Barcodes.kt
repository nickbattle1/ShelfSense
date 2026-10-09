package com.example.shelfsense.domain

// checks for the manual entry screen. the scanner validates codes itself,
// but a typed number can easily have two digits swapped
object Barcodes {

    private val allowedLengths = setOf(8, 12, 13, 14)

    fun clean(input: String): String = input.filter { it.isDigit() }

    // null when the code looks fine, otherwise a message for the field
    fun problemWith(code: String): String? = when {
        code.isEmpty() -> "Enter the number printed under the barcode"
        code.length !in allowedLengths -> "Barcodes are usually 8, 12 or 13 digits long"
        !hasValidCheckDigit(code) -> "That number doesn't add up. Check the digits and try again"
        else -> null
    }

    // standard GTIN check digit: weights of 3 and 1 alternate from the right, skipping the check digit.
    // 8 digit codes are let through because UPC-E uses a different scheme and would be wrongly rejected
    fun hasValidCheckDigit(code: String): Boolean {
        if (code.length !in allowedLengths || !code.all { it.isDigit() }) return false
        if (code.length == 8) return true
        val digits = code.map { it - '0' }
        val body = digits.dropLast(1).reversed()
        val sum = body.withIndex().sumOf { (index, digit) -> if (index % 2 == 0) digit * 3 else digit }
        return (10 - sum % 10) % 10 == digits.last()
    }
}
