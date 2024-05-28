package org.demiurg906.kotlin.plugin.fir

import com.intellij.psi.PsiElement
import com.intellij.util.containers.JBIterable.Split
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.Catch
import org.jetbrains.kotlin.util.collectionUtils.concat
import java.io.File
import kotlin.jvm.internal.Ref.IntRef

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
            val file = File("output.txt")
            printCFG(declaration.controlFlowGraphReference?.controlFlowGraph, file)
        }
    }

    fun printCFG(cfg : ControlFlowGraph?, file : File)
    {
        if (cfg == null)
            return
        file.writeText(cfg.name + "\n")
        val variableUsage = mutableMapOf<Name, Usage>()
        //printNode(cfg.enterNode, file, variableUsage, mutableSetOf())
        //file.appendText(variableUsage.toString())

        val tree = createTree(cfg.enterNode, 0, mutableMapOf(), IntRef(), UsageInformation(0, UsageInformation(-1)))
        file.appendText("{${tree.printNode()}}")
    }

    private fun createTree(cfgNode : CFGNode<*>, depth : Int, visited : MutableMap<CFGNode<*>, Node>, count : IntRef, usageInformation : UsageInformation) : Node
    {
        val node = Node(count.element, cfgNode::class.simpleName!!, depth, cfgNode, usageInformation)
        count.element = count.element.inc()
        visited[cfgNode] = node

        cfgNode.followingNodes.forEach{
            if (!visited.contains(it))
            {
                val localUsageInfo =  when (it)
                {
                    is FunctionEnterNode, is SplitPostponedLambdasNode, is AnonymousFunctionExpressionNode,
                    is BlockEnterNode, is WhenEnterNode, is LoopEnterNode, is TryExpressionEnterNode,
                    is TryMainBlockEnterNode, is CatchClauseEnterNode, is FinallyBlockEnterNode ->
                        UsageInformation(node.UsageInformation.Id + 1, node.UsageInformation)
                    is FunctionExitNode, is PostponedLambdaExitNode, is BlockExitNode, is WhenExitNode, is LoopExitNode,
                    is TryMainBlockExitNode, is CatchClauseExitNode, is FinallyBlockExitNode, is TryExpressionExitNode ->
                        if(node.UsageInformation.Parent == null){
                            node.UsageInformation
                        }
                    else
                        {
                            node.UsageInformation.Parent
                        }
                    else -> node.UsageInformation
                }

                val child = createTree(it, depth + 1, visited, count, localUsageInfo)
                child.Parents.add(node)
                node.Children.add(child)
            }
            else
            {
                visited[it]?.Parents?.add(node)
                node.Children.add(visited[it]!!)
            }
        }
        return node
    }

    fun printNode(cfgNode : CFGNode<*>, file : File, usageInformation : MutableMap<Name,Usage>, visited : MutableSet<CFGNode<*>>)
    {
        visited.add(cfgNode)
        file.appendText(cfgNode.id.toString() + "=".repeat(cfgNode.level) + cfgNode.render() + "\n")
        file.appendText("rendering ${cfgNode.followingNodes.count()} nodes\n")
        cfgNode.followingNodes.forEach{
            if(visited.contains(it)) {
                file.appendText("Backedge\n")
                return
            }
            printNode(it, file, usageInformation, visited)
        }


    }



    private fun upUsage(usage : Usage) : Usage
    {
        return when (usage) {
            Usage.BOTTOM -> Usage.ONCE
            Usage.ZERO -> Usage.ONCE
            Usage.ONCE -> Usage.AT_LEAST_ONCE
            Usage.AT_MOST_ONCE -> Usage.AT_LEAST_ONCE
            Usage.AT_LEAST_ONCE -> Usage.INFINITE
            else -> usage
        }
    }

}

class Node (val Id : Int, val Name : String, val Depth : Int, val CFGNode : CFGNode<*>, val UsageInformation : UsageInformation)
{
    val Children : MutableSet<Node> = mutableSetOf()
    val Parents : MutableSet<Node> = mutableSetOf()

    fun printNode(visited: MutableSet<Node> = mutableSetOf()) : String
    {
        if(visited.contains(this))
            return "\"Id\": $Id"
        visited.add(this)
        val children = Children.joinToString { "{ ${it.printNode(visited)}}"}
        val parents = Parents.joinToString { it.Id.toString() }
        return "\"Id\": $Id, \"name\": \"${this.CFGNode::class.simpleName}\", \"depth\": ${this.UsageInformation.Id}, \"children\": [$children], \"parents\": [$parents]"
    }

    fun toGraph(visited : MutableSet<Node> = mutableSetOf()) : String
    {
        if(visited.contains(this))
            return ""
        visited.add(this)
        var result = this.Id.toString() + "\n"
        this.Children.forEach{
            result += "${this.Id} ${it.Id}\n"
            result += it.toGraph(visited) + "\n"
        }
        return result
    }
}

enum class Usage
{
    BOTTOM, ZERO, ONCE, INFINITE , AT_MOST_ONCE, AT_LEAST_ONCE, TOP
}
class UsageInformation (val Id: Int, val Parent : UsageInformation? = null)
{
    var UsageAmount: Usage = Usage.BOTTOM
    val Variables : MutableMap<String, UsageInformation> = mutableMapOf()
}