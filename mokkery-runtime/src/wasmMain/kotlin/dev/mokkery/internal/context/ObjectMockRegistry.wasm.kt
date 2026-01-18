package dev.mokkery.internal.context

import dev.mokkery.annotations.DelicateMokkeryApi
import dev.mokkery.answering.Answer
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace

/**
 * Wasm implementation of ObjectMockRegistry.
 * Wasm is single-threaded, so we use a simple global registry.
 */
@PublishedApi
internal actual class ObjectMockRegistry @PublishedApi internal actual constructor() {
    private val impl = ObjectMockRegistryImpl()

    actual companion object {
        private var instance: ObjectMockRegistry? = null

        @PublishedApi
        internal actual fun current(): ObjectMockRegistry {
            return instance ?: ObjectMockRegistry().also { instance = it }
        }

        actual fun clearCurrent() {
            instance?.clearAll()
            instance = null
        }
    }

    @PublishedApi
    internal actual fun activate(objectId: String) = impl.activate(objectId)

    actual fun isActive(objectId: String): Boolean = impl.isActive(objectId)

    @PublishedApi
    internal actual fun deactivate(objectId: String) = impl.deactivate(objectId)

    @OptIn(DelicateMokkeryApi::class)
    actual fun registerStub(objectId: String, signature: FunctionSignature, answer: Answer<*>) =
        impl.registerStub(objectId, signature, answer)

    @OptIn(DelicateMokkeryApi::class)
    actual fun resolveStub(objectId: String, signature: FunctionSignature): Answer<*>? =
        impl.resolveStub(objectId, signature)

    actual fun traceCall(trace: ObjectCallTrace) = impl.traceCall(trace)

    actual fun getTraces(objectId: String): List<ObjectCallTrace> = impl.getTraces(objectId)

    actual fun getAllTraces(): List<ObjectCallTrace> = impl.getAllTraces()

    actual fun clearAll() = impl.clearAll()
}
