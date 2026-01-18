package dev.mokkery.internal.context

import dev.mokkery.answering.Answer
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace
import kotlin.native.concurrent.ThreadLocal

// Top-level ThreadLocal storage for Native platform
@ThreadLocal
private var objectMockRegistryInstance: ObjectMockRegistry? = null

/**
 * Native implementation of ObjectMockRegistry using Kotlin/Native's ThreadLocal.
 * Each thread gets its own registry, ensuring parallel test execution is isolated.
 */
@PublishedApi
internal actual class ObjectMockRegistry @PublishedApi internal actual constructor() {
    private val impl = ObjectMockRegistryImpl()

    actual companion object {

        @PublishedApi
        internal actual fun current(): ObjectMockRegistry {
            return objectMockRegistryInstance ?: ObjectMockRegistry().also { objectMockRegistryInstance = it }
        }

        actual fun clearCurrent() {
            objectMockRegistryInstance?.clearAll()
            objectMockRegistryInstance = null
        }
    }

    @PublishedApi
    internal actual fun activate(objectId: String) = impl.activate(objectId)

    actual fun isActive(objectId: String): Boolean = impl.isActive(objectId)

    @PublishedApi
    internal actual fun deactivate(objectId: String) = impl.deactivate(objectId)

    actual fun registerStub(objectId: String, signature: FunctionSignature, answer: Answer<*>) =
        impl.registerStub(objectId, signature, answer)

    actual fun resolveStub(objectId: String, signature: FunctionSignature): Answer<*>? =
        impl.resolveStub(objectId, signature)

    actual fun traceCall(trace: ObjectCallTrace) = impl.traceCall(trace)

    actual fun getTraces(objectId: String): List<ObjectCallTrace> = impl.getTraces(objectId)

    actual fun getAllTraces(): List<ObjectCallTrace> = impl.getAllTraces()

    actual fun clearAll() = impl.clearAll()
}
