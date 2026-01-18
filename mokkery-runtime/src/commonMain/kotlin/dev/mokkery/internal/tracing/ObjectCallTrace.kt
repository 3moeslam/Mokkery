package dev.mokkery.internal.tracing

import dev.mokkery.context.CallArgument
import dev.mokkery.internal.MokkeryInstanceId
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.utils.description
import kotlin.reflect.KClass

/**
 * Records a function invocation on a mocked object for verification.
 *
 * Similar to [CallTrace] but specifically for object mocking, using
 * [FunctionSignature] and objectId instead of instanceId.
 *
 * @property objectId Fully qualified name of the object (e.g., "com.example.MyService")
 * @property signature The function that was called
 * @property arguments Actual argument values passed to the function
 * @property timestamp When the call occurred (for ordering)
 * @property contextId Unique identifier for the test execution context
 */
internal data class ObjectCallTrace(
    val objectId: String,
    val signature: FunctionSignature,
    val arguments: List<Any?>,
    val timestamp: Long,
    val contextId: Long = 0L,
) : Comparable<ObjectCallTrace> {

    override fun compareTo(other: ObjectCallTrace): Int = timestamp.compareTo(other.timestamp)

    override fun toString(): String = buildString {
        append(objectId)
        append(".")
        append(signature.name)
        append("(")
        append(arguments.joinToString { it.description() })
        append(")")
    }

    /**
     * Returns a string representation without the objectId prefix.
     */
    fun toStringNoObjectId(): String = buildString {
        append(signature.name)
        append("(")
        append(arguments.joinToString { it.description() })
        append(")")
    }

    /**
     * Converts this ObjectCallTrace to a CallTrace for verification compatibility.
     */
    fun toCallTrace(): CallTrace = CallTrace(
        instanceId = MokkeryInstanceId(objectId, 0),
        name = signature.name,
        args = signature.parameterNames.mapIndexed { index, paramName ->
            CallArgument(
                value = arguments.getOrNull(index),
                name = paramName,
                type = signature.parameterTypes.getOrNull(index)?.toKClassOrAny() ?: Any::class,
                isVararg = false
            )
        },
        orderStamp = timestamp
    )
}

/**
 * Converts a rendered type string to its KClass representation.
 * Falls back to Any::class for unknown or complex types.
 */
private fun String.toKClassOrAny(): KClass<*> = when (this) {
    "kotlin.String" -> String::class
    "kotlin.Int" -> Int::class
    "kotlin.Long" -> Long::class
    "kotlin.Short" -> Short::class
    "kotlin.Byte" -> Byte::class
    "kotlin.Float" -> Float::class
    "kotlin.Double" -> Double::class
    "kotlin.Boolean" -> Boolean::class
    "kotlin.Char" -> Char::class
    "kotlin.Unit" -> Unit::class
    "kotlin.Any" -> Any::class
    "kotlin.Any?" -> Any::class
    "kotlin.Nothing" -> Nothing::class
    else -> Any::class // Fallback for complex types, generics, etc.
}
