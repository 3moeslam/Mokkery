package dev.mokkery.internal.context

import dev.mokkery.answering.Answer
import dev.mokkery.internal.Counter
import dev.mokkery.internal.MonotonicCounter
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * Platform-specific registry for object mock state.
 *
 * Each test execution context has its own registry, enabling parallel test isolation.
 * The registry manages active mock scopes, stub registrations, and call traces.
 */
@PublishedApi
internal expect class ObjectMockRegistry @PublishedApi internal constructor() {

    companion object {
        /**
         * Gets the registry for the current execution context.
         * Creates a new one if none exists.
         */
        @PublishedApi
        internal fun current(): ObjectMockRegistry

        /**
         * Clears all object mocks in the current context.
         */
        fun clearCurrent()
    }

    /**
     * Activates mocking for an object.
     * @param objectId Fully qualified name of the object (e.g., "com.example.MyService")
     */
    @PublishedApi
    internal fun activate(objectId: String)

    /**
     * Checks if an object is currently mocked.
     */
    fun isActive(objectId: String): Boolean

    /**
     * Deactivates mocking for an object.
     */
    @PublishedApi
    internal fun deactivate(objectId: String)

    /**
     * Registers a stub for an object function.
     */
    fun registerStub(objectId: String, signature: FunctionSignature, answer: Answer<*>)

    /**
     * Resolves the stub for an object function call.
     * @return The answer, or null if no stub configured
     */
    fun resolveStub(objectId: String, signature: FunctionSignature): Answer<*>?

    /**
     * Records a function call for later verification.
     */
    fun traceCall(trace: ObjectCallTrace)

    /**
     * Gets all traces for an object.
     */
    fun getTraces(objectId: String): List<ObjectCallTrace>

    /**
     * Gets all traces across all mocked objects.
     */
    fun getAllTraces(): List<ObjectCallTrace>

    /**
     * Clears all mocks in this registry.
     */
    fun clearAll()
}

/**
 * Common implementation logic for ObjectMockRegistry.
 * Platform implementations delegate to this class.
 */
internal class ObjectMockRegistryImpl {
    private val lock = reentrantLock()
    private val scopes = linkedMapOf<String, ObjectMockScope>()
    private val counter: Counter = MonotonicCounter(0L)
    private val contextId = contextIdGenerator.getAndIncrement()

    fun activate(objectId: String) {
        lock.withLock {
            if (objectId !in scopes) {
                scopes[objectId] = ObjectMockScope(
                    objectId = objectId,
                    contextId = contextId,
                    createdAt = counter.next()
                )
            }
        }
    }

    fun isActive(objectId: String): Boolean = lock.withLock { objectId in scopes }

    fun deactivate(objectId: String) {
        lock.withLock {
            scopes.remove(objectId)?.clear()
        }
    }

    fun registerStub(objectId: String, signature: FunctionSignature, answer: Answer<*>) {
        lock.withLock {
            scopes[objectId]?.registerStub(signature, answer)
        }
    }

    fun resolveStub(objectId: String, signature: FunctionSignature): Answer<*>? {
        return lock.withLock { scopes[objectId]?.resolveStub(signature) }
    }

    fun traceCall(trace: ObjectCallTrace) {
        lock.withLock {
            scopes[trace.objectId]?.recordTrace(trace)
        }
    }

    fun getTraces(objectId: String): List<ObjectCallTrace> {
        return lock.withLock { scopes[objectId]?.traces ?: emptyList() }
    }

    fun getAllTraces(): List<ObjectCallTrace> {
        return lock.withLock {
            scopes.values.flatMap { it.traces }.sortedBy { it.timestamp }
        }
    }

    fun clearAll() {
        lock.withLock {
            scopes.values.forEach { it.clear() }
            scopes.clear()
        }
    }

    companion object {
        private val contextIdGenerator = atomic(0L)
    }
}
