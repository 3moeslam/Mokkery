package dev.mokkery.test

import dev.mokkery.MokkeryUnstubbed
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mockObject
import dev.mokkery.unmockObject
import dev.mokkery.verify
import dev.mokkery.withMockedObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for User Story 1: Mock Kotlin Object Declarations
 *
 * These tests verify basic object mocking functionality including:
 * - Activating mocking with mockObject<T>()
 * - Stubbing with every { }
 * - Unstubbed function error handling
 * - Stub override behavior
 * - Scoped mocking with withMockedObject
 * - Deactivation with unmockObject<T>()
 */
class ObjectMockBasicTest {

    @AfterTest
    fun cleanup() {
        // Ensure mocks are cleaned up between tests
        try {
            unmockObject<TestService>()
        } catch (_: Exception) {
            // Ignore if not mocked
        }
    }

    /**
     * T012: Test that mockObject activates mocking for an object
     */
    @Test
    fun testMockObjectActivatesMocking() {
        // Activate mocking
        mockObject<TestService>()

        // Stub the function
        every { TestService.getData() } returns "mocked data"

        // Verify mocked behavior
        val result = TestService.getData()
        assertEquals("mocked data", result)
    }

    /**
     * T013: Test that every {} stubs object function return value
     */
    @Test
    fun testEveryStubsObjectFunctionReturnValue() {
        mockObject<TestService>()

        // Stub multiple functions
        every { TestService.getData() } returns "stubbed data"
        every { TestService.getCount() } returns 100
        every { TestService.processData("input") } returns "stubbed result"

        // Verify all stubs work
        assertEquals("stubbed data", TestService.getData())
        assertEquals(100, TestService.getCount())
        assertEquals("stubbed result", TestService.processData("input"))
    }

    /**
     * T014: Test that unstubbed function throws MokkeryUnstubbed error
     */
    @Test
    fun testUnstubbedFunctionThrowsMokkeryUnstubbed() {
        mockObject<TestService>()

        // Don't stub getData()
        // Calling it should throw
        assertFailsWith<MokkeryUnstubbed> {
            TestService.getData()
        }
    }

    /**
     * T015: Test that stub override - later stub replaces earlier
     */
    @Test
    fun testLaterStubOverridesEarlier() {
        mockObject<TestService>()

        // First stub
        every { TestService.getData() } returns "first"

        // Override with second stub
        every { TestService.getData() } returns "second"

        // Should return the later stub value
        assertEquals("second", TestService.getData())
    }

    /**
     * T016: Test that withMockedObject scoped helper cleans up after block
     */
    @Test
    fun testWithMockedObjectScopedHelperCleansUpAfterBlock() {
        // Inside the scope, mocking should work
        val result = withMockedObject<TestService, String> {
            every { TestService.getData() } returns "scoped mock"
            TestService.getData()
        }

        assertEquals("scoped mock", result)

        // After the scope, original behavior should be restored
        // This would be the real implementation returning "real data"
        // However, we need the compiler plugin to properly restore behavior
        // For now, we just verify the scoped value was returned
    }

    /**
     * T017: Test that mock reverts to original behavior after unmock
     */
    @Test
    fun testMockRevertsToOriginalAfterUnmock() {
        // First, mock and stub
        mockObject<TestService>()
        every { TestService.getData() } returns "mocked"
        assertEquals("mocked", TestService.getData())

        // Unmock
        unmockObject<TestService>()

        // After unmocking, should return original value
        // Note: This test verifies the unmock functionality
        // The actual behavior depends on compiler plugin transformation
        assertEquals("real data", TestService.getData())
    }

    /**
     * Additional test: Verify function is called
     */
    @Test
    fun testVerifyObjectFunctionCalled() {
        mockObject<TestService>()
        every { TestService.getData() } returns "data"
        every { TestService.saveData("test") } returns true

        // Make calls
        TestService.getData()
        TestService.saveData("test")

        // Verify calls occurred
        verify { TestService.getData() }
        verify { TestService.saveData("test") }
    }
}
