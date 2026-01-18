package dev.mokkery.plugin.transformers

import dev.mokkery.plugin.core.CompilerPluginScope
import dev.mokkery.plugin.core.CoreTransformer
import dev.mokkery.plugin.core.Mokkery
import dev.mokkery.plugin.core.getClass
import dev.mokkery.plugin.ir.irCall
import dev.mokkery.plugin.ir.irCallConstructor
import dev.mokkery.plugin.ir.irCallListOf
import dev.mokkery.plugin.ir.irLambdaOf
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render

/**
 * Transforms object function bodies to inject interceptor checks.
 *
 * For each function in an object declaration, this transformer:
 * 1. Wraps the original function body in a lambda
 * 2. Calls ObjectInterceptor.intercept() with the function signature
 * 3. The interceptor checks if mocking is active and either:
 *    - Returns stubbed value if configured
 *    - Calls original implementation otherwise
 */
class ObjectTransformer(compilerPluginScope: CompilerPluginScope) : CoreTransformer(compilerPluginScope) {

    private val objectInterceptorClass by lazy { getClass(Mokkery.Class.ObjectInterceptor) }
    private val functionSignatureClass by lazy { getClass(Mokkery.Class.FunctionSignature) }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.isObject) return super.visitClassNew(declaration)
        if (declaration.companionObject() == declaration) return super.visitClassNew(declaration)

        // Skip Mokkery's internal classes to prevent infinite recursion
        // But allow dev.mokkery.test for testing purposes
        val fqName = declaration.kotlinFqName.asString()
        if (fqName.startsWith("dev.mokkery.") && !fqName.startsWith("dev.mokkery.test")) {
            return super.visitClassNew(declaration)
        }

        transformObjectFunctions(declaration)
        return super.visitClassNew(declaration)
    }

    private fun transformObjectFunctions(objectClass: IrClass) {
        val objectId = objectClass.kotlinFqName.asString()

        objectClass.functions
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.body != null && !it.isFakeOverride }
            .forEach { function ->
                transformFunction(function, objectId)
            }
    }

    private fun getInterceptResultClass(): IrClass = getClass(Mokkery.Class.ObjectInterceptResult)
    private fun getMockedClass(): IrClass = getInterceptResultClass().nestedClasses.first { it.name.asString() == "Mocked" }

    private fun transformFunction(function: IrSimpleFunction, objectId: String) {
        val originalBody = function.body as? IrBlockBody ?: return

        // Don't transform property accessors that would cause infinite recursion
        if (function.correspondingPropertySymbol != null) return

        val isSuspend = function.isSuspend
        val interceptFunName = if (isSuspend) "interceptCall" else "interceptCallBlocking"

        // Get the simplified intercept function from ObjectInterceptor
        val interceptFun = objectInterceptorClass.functions
            .first { it.name.asString() == interceptFunName }

        function.body = DeclarationIrBuilder(pluginContext, function.symbol, function.startOffset, function.endOffset).irBlockBody {
            // Create FunctionSignature with parameter names for verification matching
            val signatureExpr = irCallConstructor(functionSignatureClass.primaryConstructor!!) {
                arguments[0] = irString(function.name.asString())
                arguments[1] = irCallListOf(
                    transformerScope = this@ObjectTransformer,
                    type = pluginContext.irBuiltIns.stringType,
                    elements = function.nonDispatchParameters.map { irString(it.type.render()) }
                )
                arguments[2] = irCallListOf(
                    transformerScope = this@ObjectTransformer,
                    type = pluginContext.irBuiltIns.stringType,
                    elements = function.nonDispatchParameters.map { irString(it.name.asString()) }
                )
                arguments[3] = irString(function.returnType.render())
            }

            // Create args list
            val argsExpr = irCallListOf(
                transformerScope = this@ObjectTransformer,
                type = pluginContext.irBuiltIns.anyNType,
                elements = function.nonDispatchParameters.map { irGet(it) }
            )

            // Call ObjectInterceptor.interceptCall to check if mocked
            val interceptCall = irCall(interceptFun) {
                // Dispatch receiver for the ObjectInterceptor object
                arguments[0] = irGetObject(objectInterceptorClass.symbol)
                // Function parameters
                arguments[1] = irString(objectId)
                arguments[2] = signatureExpr
                arguments[3] = argsExpr
            }

            // Store the result
            val resultVar = createTmpVariable(interceptCall, nameHint = "interceptResult")

            // Check if result is Mocked (not NotMocked)
            val mockedClass = getMockedClass()
            +irIfThenElse(
                type = function.returnType,
                condition = irIs(irGet(resultVar), mockedClass.defaultType),
                thenPart = irBlock {
                    // Return the mocked value
                    val mockedResult = irAs(irGet(resultVar), mockedClass.defaultType)
                    val valueProperty = mockedClass.properties.first { it.name.asString() == "value" }
                    val valueGetter = valueProperty.getter!!
                    val value = irCall(valueGetter) {
                        arguments[0] = mockedResult
                    }
                    +irReturn(irAs(value, function.returnType))
                },
                elsePart = irBlock {
                    // Execute original body
                    originalBody.statements.forEach { +it }
                }
            )
        }
    }
}
