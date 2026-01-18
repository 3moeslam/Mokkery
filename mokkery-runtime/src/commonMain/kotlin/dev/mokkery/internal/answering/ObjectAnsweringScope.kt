package dev.mokkery.internal.answering

import dev.mokkery.annotations.DelicateMokkeryApi
import dev.mokkery.answering.Answer
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.internal.context.ObjectMockRegistry
import dev.mokkery.internal.signature.FunctionSignature
import dev.mokkery.internal.templating.CallTemplate
import dev.mokkery.internal.utils.bestName

/**
 * Answering scope for object mocking.
 *
 * Registers answers with the ObjectMockRegistry instead of the mock's AnsweringRegistry.
 */
internal class ObjectAnsweringScope<T>(
    private val objectId: String,
    private val template: CallTemplate,
) : SuspendAnsweringScope<T>, BlockingAnsweringScope<T> {

    @DelicateMokkeryApi
    override fun answers(answer: Answer<T>) {
        val registry = ObjectMockRegistry.current()
        val signature = FunctionSignature(
            name = template.name,
            parameterTypes = template.parameters.map { it.type.bestName() },
            returnType = "kotlin.Any" // We don't know the exact return type here
        )
        registry.registerStub(objectId, signature, answer)
    }
}
