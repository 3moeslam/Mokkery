@file:Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")

package dev.mokkery

import dev.mokkery.internal.context.ObjectMockRegistry
import dev.mokkery.internal.utils.mokkeryIntrinsic

/**
 * Activates mocking for the specified Kotlin `object` declaration.
 *
 * After calling this, function calls on [T] will be intercepted and can be
 * stubbed with [every] and verified with [verify].
 *
 * The mock is automatically cleaned up when the test context ends.
 *
 * [T] **must** be a Kotlin `object` declaration (singleton) and **cannot** be:
 * * An interface (use [mock] instead)
 * * A class (use [mock] instead)
 * * A generic type parameter
 *
 * @param T The object type to mock (must be a Kotlin object declaration)
 * @throws MokkeryRuntimeException if T is not a valid object declaration
 *
 * @sample
 * ```
 * object MyService {
 *     fun getData(): String = "real data"
 * }
 *
 * @Test
 * fun testWithMockedObject() {
 *     mockObject<MyService>()
 *     every { MyService.getData() } returns "mocked"
 *     assertEquals("mocked", MyService.getData())
 *     verify { MyService.getData() }
 * }
 * ```
 */
public inline fun <reified T : Any> mockObject(): Unit = mokkeryIntrinsic

/**
 * Activates mocking for the specified Kotlin `object` declaration within a [MokkerySuiteScope].
 *
 * This variant allows the mock to be associated with a test suite for automatic cleanup.
 *
 * @param T The object type to mock (must be a Kotlin object declaration)
 * @see mockObject
 */
public inline fun <reified T : Any> MokkerySuiteScope.mockObject(): Unit = mokkeryIntrinsic

/**
 * Deactivates mocking for the specified object declaration.
 *
 * After calling this, function calls on [T] will execute the original implementation.
 * Usually not needed as cleanup is automatic when the test context ends.
 *
 * @param T The object type to unmock
 *
 * @sample
 * ```
 * mockObject<MyService>()
 * every { MyService.getData() } returns "mocked"
 * // ... test code ...
 * unmockObject<MyService>()
 * // MyService.getData() now returns real data
 * ```
 */
public inline fun <reified T : Any> unmockObject(): Unit = mokkeryIntrinsic

/**
 * Deactivates mocking for the specified object declaration within a [MokkerySuiteScope].
 *
 * @param T The object type to unmock
 * @see unmockObject
 */
public inline fun <reified T : Any> MokkerySuiteScope.unmockObject(): Unit = mokkeryIntrinsic

/**
 * Executes [block] with the specified object mocked, then automatically cleans up.
 *
 * This is the recommended way to mock objects when you want automatic cleanup
 * after the block completes, regardless of whether it succeeds or throws.
 *
 * @param T The object type to mock (must be a Kotlin object declaration)
 * @param R The return type of the block
 * @param block The test code to execute with the object mocked
 * @return The result of [block]
 *
 * @sample
 * ```
 * val result = withMockedObject<MyService, String> {
 *     every { MyService.getData() } returns "scoped mock"
 *     MyService.getData()
 * }
 * // Mock automatically cleaned up here
 * assertEquals("scoped mock", result)
 * ```
 */
public inline fun <reified T : Any, R> withMockedObject(block: () -> R): R {
    val objectId = T::class.qualifiedName ?: error("Cannot get qualified name for ${T::class}")
    val registry = ObjectMockRegistry.current()
    registry.activate(objectId)
    try {
        return block()
    } finally {
        registry.deactivate(objectId)
    }
}

/**
 * Executes [block] with the specified object mocked within a [MokkerySuiteScope].
 *
 * @param T The object type to mock
 * @param R The return type of the block
 * @param block The test code to execute with the object mocked
 * @return The result of [block]
 * @see withMockedObject
 */
public inline fun <reified T : Any, R> MokkerySuiteScope.withMockedObject(block: () -> R): R {
    val objectId = T::class.qualifiedName ?: error("Cannot get qualified name for ${T::class}")
    val registry = ObjectMockRegistry.current()
    registry.activate(objectId)
    try {
        return block()
    } finally {
        registry.deactivate(objectId)
    }
}
