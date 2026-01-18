package dev.mokkery.internal.interceptor

import dev.mokkery.MokkeryUnstubbed
import dev.mokkery.answering.Answer
import dev.mokkery.internal.Counter
import dev.mokkery.internal.MonotonicCounter
import dev.mokkery.internal.context.ObjectMockRegistry
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.tracing.ObjectCallTrace
import dev.mokkery.internal.utils.unsafeCast

/**
 * Result of object interception. Either [NotMocked] to indicate the original
 * implementation should run, or [Mocked] with the stubbed value.
 */
internal sealed class ObjectInterceptResult {
    /** Object is not currently mocked - run original implementation */
    object NotMocked : ObjectInterceptResult()

    /** Object is mocked - use this value */
    class Mocked(val value: Any?) : ObjectInterceptResult()
}

/**
 * Interceptor logic for object function calls.
 *
 * This object is called by the generated code injected into object function bodies
 * by the compiler plugin. It checks if the object is currently mocked and either:
 * - Returns the stubbed value if a stub is configured
 * - Throws [MokkeryUnstubbed] if no stub is configured
 * - Returns null if the object is not mocked (caller should run original code)
 */
internal object ObjectInterceptor {

    private val timestampCounter: Counter = MonotonicCounter(0L)

    /**
     * Intercepts a blocking (non-suspend) object function call.
     *
     * @param objectId Fully qualified object name (e.g., "com.example.MyService")
     * @param signature Function being called
     * @param args Actual argument values
     * @return [ObjectInterceptResult.NotMocked] if not mocked, [ObjectInterceptResult.Mocked] with value if mocked
     * @throws MokkeryUnstubbed if object is mocked but function has no stub
     */
    fun interceptCallBlocking(
        objectId: String,
        signature: FunctionSignature,
        args: List<Any?>,
    ): ObjectInterceptResult {
        val registry = ObjectMockRegistry.current()

        if (!registry.isActive(objectId)) {
            return ObjectInterceptResult.NotMocked
        }

        // Record the call for verification
        registry.traceCall(
            ObjectCallTrace(
                objectId = objectId,
                signature = signature,
                arguments = args,
                timestamp = timestampCounter.next()
            )
        )

        // Resolve stub or throw
        val answer = registry.resolveStub(objectId, signature)
            ?: throw MokkeryUnstubbed(objectId, signature)

        // Execute the answer
        val result = when (answer) {
            is Answer.Const<*> -> answer.value
            is Answer.Throws -> throw answer.throwable
            else -> error("Unsupported answer type for object mocking: ${answer::class}. " +
                    "Currently only 'returns' and 'throws' are supported.")
        }
        return ObjectInterceptResult.Mocked(result)
    }

    /**
     * Intercepts a suspend object function call.
     *
     * @param objectId Fully qualified object name (e.g., "com.example.MyService")
     * @param signature Function being called
     * @param args Actual argument values
     * @return [ObjectInterceptResult.NotMocked] if not mocked, [ObjectInterceptResult.Mocked] with value if mocked
     * @throws MokkeryUnstubbed if object is mocked but function has no stub
     */
    fun interceptCall(
        objectId: String,
        signature: FunctionSignature,
        args: List<Any?>,
    ): ObjectInterceptResult {
        val registry = ObjectMockRegistry.current()

        if (!registry.isActive(objectId)) {
            return ObjectInterceptResult.NotMocked
        }

        // Record the call for verification
        registry.traceCall(
            ObjectCallTrace(
                objectId = objectId,
                signature = signature,
                arguments = args,
                timestamp = timestampCounter.next()
            )
        )

        // Resolve stub or throw
        val answer = registry.resolveStub(objectId, signature)
            ?: throw MokkeryUnstubbed(objectId, signature)

        // Execute the answer
        val result = when (answer) {
            is Answer.Const<*> -> answer.value
            is Answer.Throws -> throw answer.throwable
            else -> error("Unsupported answer type for object mocking: ${answer::class}. " +
                    "Currently only 'returns' and 'throws' are supported.")
        }
        return ObjectInterceptResult.Mocked(result)
    }
}
