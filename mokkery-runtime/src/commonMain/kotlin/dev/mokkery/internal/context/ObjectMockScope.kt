package dev.mokkery.internal.context

import dev.mokkery.answering.Answer
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * Represents an active mocking session for a specific object declaration within a test context.
 *
 * This scope holds all stub behaviors and recorded call traces for a single mocked object.
 * Each test execution context has its own scope instances, ensuring isolation.
 *
 * @property objectId Fully qualified name of the object (e.g., "com.example.MyService")
 * @property contextId Unique identifier for the test execution context
 * @property createdAt Timestamp when this scope was created (for debugging/ordering)
 */
internal class ObjectMockScope(
    val objectId: String,
    val contextId: Long,
    val createdAt: Long,
) {
    private val lock = reentrantLock()
    private val _stubs = linkedMapOf<FunctionSignature, Answer<*>>()
    private val _traces = mutableListOf<ObjectCallTrace>()

    /**
     * Returns a snapshot of all registered stubs.
     * The returned map is a copy to ensure thread safety.
     */
    val stubs: Map<FunctionSignature, Answer<*>>
        get() = lock.withLock { _stubs.toMap() }

    /**
     * Returns a snapshot of all recorded traces.
     * The returned list is a copy to ensure thread safety.
     */
    val traces: List<ObjectCallTrace>
        get() = lock.withLock { _traces.toList() }

    /**
     * Registers a stub behavior for a function.
     * If a stub already exists for this signature, it will be replaced (last-wins semantics).
     */
    fun registerStub(signature: FunctionSignature, answer: Answer<*>) {
        lock.withLock {
            _stubs[signature] = answer
        }
    }

    /**
     * Resolves the stub for a function call.
     * Matches by function name only for now (parameters matching will be added later).
     * @return The answer, or null if no stub is configured
     */
    fun resolveStub(signature: FunctionSignature): Answer<*>? {
        return lock.withLock {
            // First try exact match
            _stubs[signature]
                // Fall back to name-only match
                ?: _stubs.entries.find { it.key.name == signature.name }?.value
        }
    }

    /**
     * Records a function invocation for later verification.
     */
    fun recordTrace(trace: ObjectCallTrace) {
        lock.withLock {
            _traces.add(trace)
        }
    }

    /**
     * Clears all stubs and traces.
     */
    fun clear() {
        lock.withLock {
            _stubs.clear()
            _traces.clear()
        }
    }

    override fun toString(): String = "ObjectMockScope(objectId='$objectId', contextId=$contextId, stubs=${_stubs.size}, traces=${_traces.size})"
}
