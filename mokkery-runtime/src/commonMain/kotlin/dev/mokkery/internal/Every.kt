@file:Suppress("unused")

package dev.mokkery.internal

import dev.mokkery.MokkeryScope
import dev.mokkery.answering.BlockingAnsweringScope
import dev.mokkery.answering.SuspendAnsweringScope
import dev.mokkery.internal.annotations.Templating
import dev.mokkery.internal.answering.ObjectAnsweringScope
import dev.mokkery.internal.answering.UnifiedAnsweringScope
import dev.mokkery.internal.answering.answering
import dev.mokkery.internal.context.ObjectMockRegistry
import dev.mokkery.internal.templating.createTemplatingScope
import dev.mokkery.internal.templating.templatingRegistry
import dev.mokkery.internal.utils.runSuspension
import dev.mokkery.internal.utils.unsafeCast
import dev.mokkery.templating.MokkeryTemplatingScope

internal fun <T> internalEverySuspend(
    block: @Templating suspend MokkeryTemplatingScope.() -> Unit
): SuspendAnsweringScope<T> = internalEvery<T> { runSuspension { block() } }.unsafeCast()

internal fun <T> internalEvery(
    block: @Templating MokkeryTemplatingScope.() -> Unit
): BlockingAnsweringScope<T> {
    val scope = MokkeryScope.global.createTemplatingScope()
    scope.apply(block)
    val registry = scope.templatingRegistry
    val template = registry.templates.singleOrNull() ?: throw NotSingleCallInEveryBlockException()

    // Check if this is an object mock (objectId will be a fully qualified name like "com.example.Service")
    // For object mocks, we use the typeName as the objectId
    val objectRegistry = ObjectMockRegistry.current()
    val objectId = template.instanceId.typeName
    if (objectRegistry.isActive(objectId)) {
        // Object mocking - use ObjectAnsweringScope
        return ObjectAnsweringScope(objectId, template)
    }

    // Regular mock
    val instanceScope = registry.collection.getScope(template.instanceId)
    return UnifiedAnsweringScope(instanceScope.answering, template)
}
