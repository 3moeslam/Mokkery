package dev.mokkery

import dev.mokkery.internal.signature.FunctionSignature

/**
 * Thrown when an unstubbed function is called on a mocked object.
 *
 * This exception indicates that a mock object was activated, but the called function
 * was not configured with a stub using `every { }`. This follows fail-fast behavior
 * to help identify missing stub configurations during test execution.
 *
 * @property objectId The fully qualified name of the mocked object
 * @property functionName The name of the function that was called
 */
public class MokkeryUnstubbed internal constructor(
    public val objectId: String,
    public val functionName: String,
    message: String,
) : MokkeryRuntimeException(message) {

    internal companion object {
        internal operator fun invoke(objectId: String, signature: FunctionSignature): MokkeryUnstubbed {
            val shortObjectId = objectId.substringAfterLast('.')
            val functionCall = signature.toShortString()
            val message = buildString {
                append("No stub configured for ")
                append(objectId)
                append(".")
                append(functionCall)
                appendLine(".")
                appendLine()
                append("Configure with: every { ")
                append(shortObjectId)
                append(".")
                append(functionCall)
                append(" } returns <value>")
            }
            return MokkeryUnstubbed(objectId, signature.name, message)
        }
    }
}
