package org.demiurg906.kotlin.plugin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

object PluginErrors {
    val FUNCTION_WITH_DUMMY_NAME by warning1<PsiElement, FirFunctionSymbol<*>>(SourceElementPositioningStrategies.DECLARATION_NAME)

    init {
        RootDiagnosticRendererFactory.registerFactory(PluginRenderer)
    }
}

object PluginRenderer: BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap = KtDiagnosticFactoryToRendererMap("Plugin").apply {
        put(PluginErrors.FUNCTION_WITH_DUMMY_NAME, "Function with dummy name: {0}", FirDiagnosticRenderers.DECLARATION_NAME)
    }
}

object DummyNameChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val name = declaration.symbol.name.asString()
        //reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_DUMMY_NAME, declaration.symbol, context)
        if (name.contains("dummy")) {
            reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_DUMMY_NAME, declaration.symbol, context)
        }
    }

}