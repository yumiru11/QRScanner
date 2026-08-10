package com.qrscanner.qrscanner.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerViewModelTest {

    @Test
    fun `http url is detected`() {
        assertTrue(ScannerViewModel.isValidUrlString("http://example.com"))
    }

    @Test
    fun `https url is detected`() {
        assertTrue(ScannerViewModel.isValidUrlString("https://example.com/path?q=1"))
    }

    @Test
    fun `plain text is not a url`() {
        assertFalse(ScannerViewModel.isValidUrlString("hello world"))
    }

    @Test
    fun `non-http scheme is not a url`() {
        assertFalse(ScannerViewModel.isValidUrlString("ftp://example.com"))
    }
}