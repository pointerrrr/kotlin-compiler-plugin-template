package org.demiurg906.kotlin.plugin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name
import java.io.File
import javax.inject.Scope
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
        val functionArgScopeInformation = ScopeInformation(true)
        val tree = createTree(cfg.enterNode, 0, mutableMapOf(), IntRef(), functionArgScopeInformation)
        file.appendText("{${tree.printNode()}}")
        val bareTree = buildBareTree(cfg.enterNode, IntRef())
        val usage = usageOnBareTree(bareTree)
    }


    private fun usageOnBareTree(bareNode : BareNode<CFGNode<*>>, visited: MutableMap<CFGNode<*>, BareNode<ScopeInformation>> = mutableMapOf()) : BareNode<ScopeInformation>
    {
        val executedAtMostOnce = bareNode.Parents.keys.fold(true) { acc, cfgNode ->
            acc && bareNode.Parents[cfgNode] == EdgeKind.Forward
        }
        val parentCount = bareNode.Parents.count{it.value == EdgeKind.Forward}
        val scopeInformation = when (bareNode.CFGNode)
        {
            is EnterNodeMarker -> {
                if(parentCount > 1) {
                    throw Exception("Parent count > 1 on EnterNodeMarker")
                }
                else if (parentCount == 0) {
                    ScopeInformation(executedAtMostOnce)
                }
                else {
                    val parentScope = visited[bareNode.Parents.keys.first().CFGNode]?.CFGNode ?: ScopeInformation(executedAtMostOnce)
                    copyScope(parentScope)
                }
            }
            is ExitNodeMarker -> {
                if (parentCount > 1) // converging branches
                {
                    val parentScopes = bareNode.Parents.filter { it.value == EdgeKind.Forward }.map{
                        visited[it.key.CFGNode]!!.CFGNode
                    }
                    mergeScopes(parentScopes)
                }
                else
                {
                    copyScope(visited[bareNode.Parents.keys.first().CFGNode]!!.CFGNode.Parent ?: throw NullPointerException())
                }
            }
            else -> {
                if(parentCount > 1) {
                    throw Error("Parent count > 1 on marker")
                }
                else {
                    copyScope(visited[bareNode.Parents.keys.first().CFGNode]!!.CFGNode)
                }
            }
        }
        bareNode.Children.forEach{
            if(it.key.Parents.count() == 1 || it.key.Parents.keys.all { visited.contains(it.CFGNode) || it.Parents[node] != EdgeKind.Forward })
            {
                val child = usageOnBareTree(it.key, visited )
            }
        }
        val node = BareNode(bareNode.Id, scopeInformation)
        return node
    }

    private fun mergeScopes(scopeInformation : Collection<ScopeInformation>) : ScopeInformation
    {
        throw NotImplementedError()
    }

    private fun copyScope(scopeInformation: ScopeInformation) : ScopeInformation
    {
        val parentScope = if (scopeInformation.Parent != null) copyScope(scopeInformation.Parent) else null
        val ret = ScopeInformation(scopeInformation.executedAtMostOnce, parentScope)
        scopeInformation.Variables.forEach{ ret.Variables[it.key] = it.value }
        return ret
    }

    // pre-condition: node is not part of visited
    private fun buildBareTree(cfgNode : CFGNode<*>, count : IntRef, visited: MutableMap<CFGNode<*>, BareNode<CFGNode<*>>> = mutableMapOf()) : BareNode<CFGNode<*>>
    {
        val node = BareNode(count.element, cfgNode)
        visited[cfgNode] = node
        count.element += 1

        cfgNode.followingNodes.forEach {
            val edge = cfgNode.edgeTo(it).kind
            if(!visited.contains(it))
            {
                val child = buildBareTree(it, count, visited)
                node.Children[child] = edge
                child.Parents[node] = edge
            }
            else
            {
                val child = visited[it]!!

                node.Children[child] = edge
                child.Parents[node] = edge
            }
        }

        return node
    }

    // pre-condition: node.CFGNode is not part of visited
    private fun calculateUsage(node : Node, visited: MutableSet<CFGNode<*>> = mutableSetOf()) : Node
    {
        val cfgNode = node.CFGNode
        val forwardParents = node.Parents.filterValues { it == EdgeKind.Forward }



        when (cfgNode)
        {
            is VariableDeclarationNode -> {
                val name = cfgNode.fir.name.asString()
                if (node.ScopeInformation.Variables.containsKey(name))
                    throw Exception("$name already exists in scope")
                node.ScopeInformation.Variables[name] =
                    UsageInformation(Usage.BOTTOM, name, node.ScopeInformation, true)
            }
            is QualifiedAccessNode -> {
                var currentScope : ScopeInformation = node.ScopeInformation
                var usedOnce = true
                val name = cfgNode.fir.calleeReference.resolved?.name?.asString()
                    ?: throw NullPointerException("could not resolve variable name")
                while(true)
                {
                    usedOnce = usedOnce && currentScope.executedAtMostOnce

                    if(currentScope.Variables.containsKey(name))
                    {
                        break
                    }
                    currentScope = currentScope.Parent ?: throw NullPointerException("variable from unknown scope")
                }
                if (usedOnce)
                {
                    currentScope.Variables[name]!!.UsageAmount = upUsage(currentScope.Variables[name]!!.UsageAmount)
                }
                else
                {
                    currentScope.Variables[name]!!.UsageAmount = Usage.INFINITE
                }
            }
            else ->
            {

            }
        }



        throw Exception()
    }

    private fun createTree(cfgNode : CFGNode<*>, depth : Int, visited : MutableMap<CFGNode<*>, Node>, count : IntRef, scopeInformation: ScopeInformation) : Node
    {
        val node = Node(count.element, cfgNode, scopeInformation)
        count.element = count.element.inc()
        visited[cfgNode] = node

        cfgNode.followingNodes.forEach{
            val edge = cfgNode.edgeTo(it)
            if (edge.kind != EdgeKind.Forward)
            {

            }
            if (!visited.contains(it))
            {
                val localUsageInfo =  when (it)
                {
                    is EnterNodeMarker -> {
                        val executedAtMostOnce = it.previousNodes.fold(true) { acc, cfgNode ->
                            acc && cfgNode.edgeTo(it).kind != EdgeKind.CfgBackward
                        }
                        ScopeInformation(executedAtMostOnce, node.ScopeInformation)
                    }
                    is ExitNodeMarker -> {

                        node.ScopeInformation.Parent ?: node.ScopeInformation
                    }
                    else -> {
                        node.ScopeInformation
                    }
                }

                val child = createTree(it, depth + 1, visited, count, localUsageInfo)
                child.Parents[node] = edge.kind
                node.Children[child] = edge.kind
            }
            else
            {
                val child = visited[it]!!
                child.Parents[node] = edge.kind
                node.Children[child] = edge.kind
            }
        }
        return node
    }

    private fun checkUsage(node: Node, visited: MutableSet<CFGNode<*>> = mutableSetOf())
    {
        val cfgNode = node.CFGNode
        if (visited.contains(cfgNode))
            return

        visited.add(cfgNode)

        when (cfgNode)
        {
            is VariableDeclarationNode ->
            {
                val name = cfgNode.fir.name.asString()
                if (node.ScopeInformation.Variables.containsKey(name))
                {
                    throw Exception("$name already exists in this scope")
                }
                else
                {
                    node.ScopeInformation.Variables[name] = UsageInformation(Usage.BOTTOM, name, node.ScopeInformation, true)
                }
            }
            is QualifiedAccessNode ->
            {
                var currentScope : ScopeInformation = node.ScopeInformation
                var usedOnce = true
                val name = cfgNode.fir.calleeReference.resolved?.name?.asString()
                    ?: throw NullPointerException("could not resolve variable name")
                while(true)
                {
                    usedOnce = usedOnce && currentScope.executedAtMostOnce

                    if(currentScope.Variables.containsKey(name))
                    {
                        break
                    }
                    currentScope = currentScope.Parent ?: throw NullPointerException("variable from unknown scope")
                }
                if (usedOnce)
                {
                    currentScope.Variables[name]!!.UsageAmount = upUsage(currentScope.Variables[name]!!.UsageAmount)
                }
                else
                {
                    currentScope.Variables[name]!!.UsageAmount = Usage.INFINITE
                }
            }
            is ExitNodeMarker -> {
                val forwardParents = node.Parents.keys.filter { node.Parents[it] == EdgeKind.Forward }

            }
            else ->
            {

            }
        }
        node.Children.keys.forEach{ key ->
            if(key.Parents.count() == 1 || key.Parents.keys.all { visited.contains(it.CFGNode) || it.Parents[node] != EdgeKind.Forward })
                checkUsage(key, visited)
        }

        return
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

class BareNode<NodeType>(val Id : Int, val CFGNode: NodeType)
{
    val Children : MutableMap<BareNode<NodeType>, EdgeKind> = mutableMapOf()
    val Parents : MutableMap<BareNode<NodeType>, EdgeKind> = mutableMapOf()
}

class Node (val Id : Int, val CFGNode : CFGNode<*>, val ScopeInformation : ScopeInformation)
{
    val Children : MutableMap<Node, EdgeKind> = mutableMapOf()
    val Parents : MutableMap<Node, EdgeKind> = mutableMapOf()

    fun printNode(visited: MutableSet<Node> = mutableSetOf()) : String
    {
        if(visited.contains(this))
            return "\"Id\": $Id"
        visited.add(this)
        val children = Children.keys.joinToString { "{ ${it.printNode(visited)}}"}
        val parents = Parents.keys.joinToString { it.Id.toString() }
        return "\"Id\": $Id, \"name\": \"${this.CFGNode::class.simpleName}\", \"children\": [$children], \"parents\": [$parents]"
    }

    fun toGraph(visited : MutableSet<Node> = mutableSetOf()) : String
    {
        if(visited.contains(this))
            return ""
        visited.add(this)
        var result = this.Id.toString() + "\n"
        this.Children.keys.forEach{
            result += "${this.Id} ${it.Id}\n"
            result += it.toGraph(visited) + "\n"
        }
        return result
    }
}

enum class Usage
{
    BOTTOM, ZERO, ONCE, AT_MOST_ONCE, AT_LEAST_ONCE, INFINITE
}

class ScopeInformation(val executedAtMostOnce: Boolean, val Parent : ScopeInformation? = null)
{
    val Variables : MutableMap<String,UsageInformation> = mutableMapOf()
}

class UsageInformation (var UsageAmount : Usage, val name : String, val Scope : ScopeInformation, val topScope : Boolean, val Parent : UsageInformation? = null)
{
    val Variables : MutableMap<String, UsageInformation> = mutableMapOf()
}
