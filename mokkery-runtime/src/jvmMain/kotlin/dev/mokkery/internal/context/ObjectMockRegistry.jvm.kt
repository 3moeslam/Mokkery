package dev.mokkery.internal.context

import dev.mokkery.answering.Answer
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace

/**
 * JVM implementation of ObjectMockRegistry using ThreadLocal for thread isolation.
 * Each thread gets its own registry, ensuring parallel test execution is isolated.
 */
@PublishedApi
internal actual class ObjectMockRegistry @PublishedApi internal actual constructor() {
    private val impl = ObjectMockRegistryImpl()

    actual companion object {
        private val threadLocal = ThreadLocal<ObjectMockRegistry>()

        @PublishedApi
        internal actual fun current(): ObjectMockRegistry {
            return threadLocal.get() ?: ObjectMockRegistry().also {
                threadLocal.set(it)
            }
        }

        actual fun clearCurrent() {
            threadLocal.get()?.clearAll()
            threadLocal.remove()
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
