package dev.mokkery.plugin

import dev.mokkery.plugin.core.CompilerPluginScope
import dev.mokkery.plugin.transformers.MokkeryTransformer
import dev.mokkery.plugin.transformers.ObjectTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class MokkeryIrGenerationExtension(
    private val config: CompilerConfiguration,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val scope = CompilerPluginScope(config, pluginContext)

        // First, transform object declarations to inject interceptor logic
        ObjectTransformer(scope).visitModuleFragment(moduleFragment)

        // Then, apply the main Mokkery transformations (mock/spy/every/verify)
        MokkeryTransformer(scope).visitModuleFragment(moduleFragment)
    }
}
