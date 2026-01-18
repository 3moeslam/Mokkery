package dev.mokkery.internal.signature

/**
 * Uniquely identifies a function within an object for stub matching.
 *
 * Unlike [dev.mokkery.context.Function], this uses string-based type names
 * for simpler comparison and storage in object mocking scenarios.
 *
 * @property name The function name
 * @property parameterTypes Fully qualified parameter type names (e.g., "kotlin.String", "kotlin.Int")
 * @property parameterNames Parameter names for trace matching (e.g., "data", "count")
 * @property returnType Fully qualified return type name
 */
internal data class FunctionSignature(
    val name: String,
    val parameterTypes: List<String>,
    val parameterNames: List<String>,
    val returnType: String,
) {
    /**
     * Creates a signature for a function with no parameters.
     */
    constructor(name: String, returnType: String) : this(name, emptyList(), emptyList(), returnType)

    /**
     * Creates a signature with parameter types only (names will be generated).
     * For backwards compatibility with existing code.
     */
    constructor(name: String, parameterTypes: List<String>, returnType: String) : this(
        name,
        parameterTypes,
        parameterTypes.mapIndexed { index, _ -> "arg$index" },
        returnType
    )

    override fun toString(): String = buildString {
        append(name)
        append("(")
        append(parameterTypes.joinToString())
        append("): ")
        append(returnType)
    }

    /**
     * Returns a short representation without return type, useful for error messages.
     */
    fun toShortString(): String = buildString {
        append(name)
        append("(")
        append(parameterTypes.joinToString())
        append(")")
    }
}
