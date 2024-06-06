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
        //file.appendText("{${tree.printNode()}}")
        val wings = boneless(cfg.enterNode)
        file.appendText(printResult(wings))

    }

    fun printResult(info : Map<CFGNode<*>, ScopeInformation>, visited : MutableSet<CFGNode<*>> = mutableSetOf()) : String
    {
        var result = ""
        info.forEach{
            result += it.key.render() + "\n" + it.value.print() + "\n"
        }

        return result
    }

    // pre-condition: cfgNode is not part of visited and all parent of cfgNode are part of visited
    private fun boneless(cfgNode : CFGNode<*>, visited: MutableMap<CFGNode<*>, ScopeInformation> = mutableMapOf()) : Map<CFGNode<*>,ScopeInformation>
    {
        val executedAtMostOnce = when (cfgNode)
        {
            is EnterNodeMarker -> {
                cfgNode.previousNodes.all{ !cfgNode.edgeFrom(it).kind.isBack}
            }
            is ExitNodeMarker -> {
                val parent = visited[cfgNode.previousNodes.first{ !cfgNode.edgeFrom(it).kind.isBack }]!!.Parent
                parent?.executedAtMostOnce ?: true
            }
            else -> {
                visited[cfgNode.previousNodes.first{ !cfgNode.edgeFrom(it).kind.isBack }]!!.executedAtMostOnce
            }
        }
        val parentCount = cfgNode.previousNodes.count{ !cfgNode.edgeFrom(it).kind.isBack  }
        val scopeInformation = when (cfgNode)
        {
            is EnterNodeMarker -> {
                if(parentCount > 1) {
                    throw Exception("Parent count > 1 on EnterNodeMarker")
                }
                else if (parentCount == 0) {
                    ScopeInformation(executedAtMostOnce)
                }
                else {
                    val parentScope = visited[cfgNode.previousNodes.first{!cfgNode.edgeFrom(it).kind.isBack}]!!
                    ScopeInformation(executedAtMostOnce, copyScope(parentScope))
                }
            }
            is ExitNodeMarker -> {
                if(parentCount == 1) {
                    val previousScope = visited[cfgNode.previousNodes.first { !cfgNode.edgeFrom(it).kind.isBack }]
                    if (previousScope?.Parent == null) {
                        ScopeInformation(true)
                    } else {
                        copyScope(previousScope.Parent)
                    }
                }
                else{
                    mergeScopes(cfgNode.previousNodes.filter { !cfgNode.edgeFrom(it).kind.isBack }.map { visited[it]!! })
                }
            }
            else -> {
                if(parentCount > 1) {
                    throw Error("Parent count > 1 on non-marker")
                }
                if(parentCount == 0) {
                    ScopeInformation(executedAtMostOnce)
                }
                else {
                    copyScope(visited[cfgNode.previousNodes.first{ !cfgNode.edgeFrom(it).kind.isBack }]!!)
                }
            }
        }
        visited[cfgNode] = scopeInformation
        when (cfgNode) {
            is VariableDeclarationNode -> {
                val name = cfgNode.fir.name.asString()
                scopeInformation.Variables[name] = UsageInformation(Usage.BOTTOM, name, true)
            }

            is QualifiedAccessNode -> {
                val name =
                    cfgNode.fir.calleeReference.resolved?.name?.asString() ?: throw NullPointerException("callee is null")
                var atMostOnce = true
                var found = false
                var current: ScopeInformation? = scopeInformation

                if (!current!!.Variables.containsKey(name)) {
                    current = current.Parent
                    while (current != null) {
                        atMostOnce = atMostOnce && current.executedAtMostOnce
                        if (current.Variables.containsKey(name)) {
                            found = true
                            break
                        }
                        current = current.Parent
                    }
                }
                if (found) {
                    if (atMostOnce) {
                        current!!.Variables[name]!!.UsageAmount = upUsage(current.Variables[name]!!.UsageAmount)
                    } else {
                        current!!.Variables[name]!!.UsageAmount = Usage.UNKNOWN
                    }
                }
            }
            else -> {}
        }
        cfgNode.followingNodes.forEach {
            if (!visited.containsKey(it))
            if (it.previousNodes.all{ prev -> visited.containsKey(prev) || it.edgeFrom(prev).kind.isBack})
                boneless(it, visited)
        }
        return visited
    }



    private fun usageOnBareTree(bareNode : BareNode<CFGNode<*>>, visited: MutableMap<CFGNode<*>,
            BareNode<ScopeInformation>> = mutableMapOf()) : BareNode<ScopeInformation>
    {
        val executedAtMostOnce = bareNode.Parents.keys.fold(true) { acc, cfgNode ->
            acc && (!bareNode.Parents[cfgNode]!!.isBack)
        }
        val parentCount = bareNode.Parents.count{!it.value.isBack}
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
                    ScopeInformation(executedAtMostOnce, copyScope(parentScope))
                }
            }
            is ExitNodeMarker -> {
                if (parentCount > 1) // converging branches
                {
                    val parentScopes = bareNode.Parents.filter { !it.value.isBack }.map{
                        visited[it.key.CFGNode]!!.CFGNode
                    }
                    mergeScopes(parentScopes)
                }
                else
                {
                    val parentNode = bareNode.Parents.keys.first().CFGNode
                    val parentScope = visited[parentNode]!!.CFGNode.Parent
                    if (parentScope == null) {
                        val nodeType = bareNode.CFGNode.edgeFrom(parentNode)
                        throw NullPointerException()
                    }
                    copyScope(parentScope)
                }
            }
            else -> {
                if(parentCount > 1) {
                    throw Error("Parent count > 1 on marker")
                }
                if(parentCount == 0) {
                    ScopeInformation(executedAtMostOnce)
                }
                else {
                    copyScope(visited[bareNode.Parents.keys.first().CFGNode]!!.CFGNode)
                }
            }
        }
        val node = BareNode(bareNode.Id, scopeInformation)
        visited[bareNode.CFGNode] = node
        bareNode.Children.forEach{
            if(it.key.Parents.count() == 1 || it.key.Parents.keys.all {
                val edgeKind = it.Parents[bareNode] ?: throw Exception()
                    visited.contains(it.CFGNode) || edgeKind.isBack })
            {
                val child = usageOnBareTree(it.key, visited)
                node.Children[child] = it.value
                child.Parents[node] = it.value
                it.key.Parents.forEach{ kvp ->
                    if(it.key.Parents.count() > 1)
                        throw Exception("")
                    val parent = visited[kvp.key.CFGNode]
                    if(parent == null) {
                        if (kvp.key.CFGNode != it.key.CFGNode)
                        {
                            throw Exception("huh")
                        }
                    }
                    else {
                        child.Parents[parent] = kvp.value
                        parent.Children[child] = kvp.value
                    }
                }
            }
        }

        return node
    }

    private fun mergeScopes(scopeInformation : Collection<ScopeInformation>) : ScopeInformation
    {
        if (scopeInformation.count() == 1)
            return scopeInformation.first()
        return merge2Scopes(scopeInformation.first(), mergeScopes(scopeInformation.drop(1)))
    }

    private fun merge2Scopes(infoA :ScopeInformation, infoB: ScopeInformation) : ScopeInformation
    {
        val parent = if(infoA.Parent == null || infoB.Parent == null) {
            null
        } else {
            merge2Scopes(infoA.Parent, infoB.Parent)
        }
        val ret = ScopeInformation(infoA.executedAtMostOnce && infoB.executedAtMostOnce, parent)
        infoA.Variables.forEach{
            ret.Variables[it.key] = copyUsageInformation(it.value)
        }
        infoB.Variables.forEach{
            val usage = copyUsageInformation(it.value)
            if(!ret.Variables.containsKey(it.key))
                ret.Variables[it.key] = usage
            else {
                ret.Variables[it.key] = mergeUsage(ret.Variables[it.key]!!, usage)
            }
        }
        return ret
    }

    private fun mergeUsage(usageA: UsageInformation, usageB: UsageInformation, parent : UsageInformation? = null) : UsageInformation {
        val mergedUsage = mergeUsage(usageA.UsageAmount, usageB.UsageAmount)
        val result = UsageInformation(mergedUsage, usageA.name, usageA.topScope, parent)
        usageA.Variables.forEach{
            result.Variables[it.key] = copyUsageInformation(it.value)
        }
        usageB.Variables.forEach{
            val copy = copyUsageInformation(it.value)
            if(result.Variables.containsKey(it.key)) {
                result.Variables[it.key] = mergeUsage(copy, it.value, result)
            }
            else {
                result.Variables[it.key] = copy
            }
        }
        return result
    }

    private fun copyScope(scopeInformation: ScopeInformation) : ScopeInformation
    {
        val parentScope = if (scopeInformation.Parent != null) copyScope(scopeInformation.Parent) else null
        val ret = ScopeInformation(scopeInformation.executedAtMostOnce, parentScope)
        scopeInformation.Variables.forEach{ ret.Variables[it.key] = copyUsageInformation(it.value) }
        return ret
    }

    private fun copyUsageInformation(usageInformation: UsageInformation) : UsageInformation {
        if (usageInformation.Parent == null)
            return UsageInformation(usageInformation.UsageAmount, usageInformation.name, usageInformation.topScope)
        return UsageInformation(usageInformation.UsageAmount, usageInformation.name, usageInformation.topScope, copyUsageInformation(usageInformation.Parent))
    }

    // pre-condition: node is not part of visited
    private fun buildBareTree(cfgNode : CFGNode<*>, count : IntRef, visited: MutableMap<CFGNode<*>, BareNode<CFGNode<*>>> = mutableMapOf()) : BareNode<CFGNode<*>>
    {
        val node = BareNode(count.element, cfgNode)
        visited[cfgNode] = node
        count.element += 1
        if(cfgNode.followingNodes.isNotEmpty()) {
            cfgNode.followingNodes.forEach {
                val edge = cfgNode.edgeTo(it).kind
                if (!visited.contains(it)) {
                    val child = buildBareTree(it, count, visited)
                    node.Children[child] = edge
                    child.Parents[node] = edge
                } else {
                    val child = visited[it]!!

                    node.Children[child] = edge
                    child.Parents[node] = edge
                }
            }
        }
        else {
            cfgNode.previousNodes.forEach {
                val edge = cfgNode.edgeFrom(it).kind
                if(!visited.contains(it))
                {

                }
            }
        }

        return node
    }

    fun printBareTree(bareNode: BareNode<CFGNode<*>>, visited: MutableSet<CFGNode<*>> = mutableSetOf()) : String
    {
        if (visited.contains(bareNode.CFGNode))
            return ""
        var result = ""
        visited.add(bareNode.CFGNode)
        bareNode.Children.keys.forEach{
            result += printBareNode(bareNode)
            result += printBareTree(it, visited)
        }
        return result
    }

    fun printBareNode(bareNode : BareNode<CFGNode<*>>) : String
    {
        var res = ""
        bareNode.Parents.forEach{
            res += "${bareNode.Id} ${it.key.Id} \n"
        }
        bareNode.Children.forEach{
            res += "${bareNode.Id} ${it.key.Id}\n"
        }
        return res
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
                    UsageInformation(Usage.BOTTOM, name,true)
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
                    currentScope.Variables[name]!!.UsageAmount = Usage.UNKNOWN
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
                    node.ScopeInformation.Variables[name] = UsageInformation(Usage.BOTTOM, name, true)
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
                    currentScope.Variables[name]!!.UsageAmount = Usage.UNKNOWN
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
            Usage.ONCE -> Usage.ONCE_OR_MORE
            Usage.AT_MOST_ONCE -> Usage.ONCE_OR_MORE
            Usage.ONCE_OR_MORE -> Usage.UNKNOWN
            else -> usage
        }
    }

    private fun mergeUsage(usageA : Usage, usageB : Usage) : Usage
    {
        return when (usageA)
        {
            Usage.BOTTOM -> {
                return when (usageB) {
                    Usage.BOTTOM -> Usage.BOTTOM
                    Usage.ZERO -> Usage.ZERO
                    Usage.ONCE -> Usage.AT_MOST_ONCE
                    Usage.INFINITE -> Usage.UNKNOWN
                    Usage.AT_MOST_ONCE -> Usage.AT_MOST_ONCE
                    Usage.ONCE_OR_MORE -> Usage.UNKNOWN
                    Usage.UNKNOWN -> Usage.UNKNOWN
                }
            }
            Usage.ZERO -> {
                return when (usageB){
                    Usage.ONCE, Usage.AT_MOST_ONCE -> Usage.AT_MOST_ONCE
                    Usage.INFINITE, Usage.ONCE_OR_MORE, Usage.UNKNOWN -> Usage.UNKNOWN
                    Usage.ZERO, Usage.BOTTOM -> Usage.ZERO
                }
            }
            Usage.ONCE -> {
                return when (usageB) {
                    Usage.BOTTOM -> Usage.AT_MOST_ONCE
                    Usage.ZERO -> Usage.AT_MOST_ONCE
                    Usage.ONCE -> Usage.ONCE
                    Usage.INFINITE -> Usage.ONCE_OR_MORE
                    Usage.AT_MOST_ONCE -> Usage.AT_MOST_ONCE
                    Usage.ONCE_OR_MORE -> Usage.ONCE_OR_MORE
                    Usage.UNKNOWN -> Usage.UNKNOWN
                }
            }
            Usage.INFINITE -> {
                return when (usageB) {
                    Usage.BOTTOM -> Usage.UNKNOWN
                    Usage.ZERO -> Usage.UNKNOWN
                    Usage.ONCE -> Usage.ONCE_OR_MORE
                    Usage.INFINITE -> Usage.INFINITE
                    Usage.AT_MOST_ONCE -> Usage.UNKNOWN
                    Usage.ONCE_OR_MORE -> Usage.ONCE_OR_MORE
                    Usage.UNKNOWN -> Usage.UNKNOWN
                }
            }
            Usage.AT_MOST_ONCE -> {
                return when (usageB) {
                    Usage.BOTTOM -> Usage.AT_MOST_ONCE
                    Usage.ZERO -> Usage.AT_MOST_ONCE
                    Usage.ONCE -> Usage.AT_MOST_ONCE
                    Usage.INFINITE -> Usage.UNKNOWN
                    Usage.AT_MOST_ONCE -> Usage.AT_MOST_ONCE
                    Usage.ONCE_OR_MORE -> Usage.UNKNOWN
                    Usage.UNKNOWN -> Usage.UNKNOWN
                }
            }
            Usage.ONCE_OR_MORE -> {
                return when (usageB) {
                    Usage.BOTTOM -> Usage.UNKNOWN
                    Usage.ZERO -> Usage.UNKNOWN
                    Usage.ONCE -> Usage.ONCE_OR_MORE
                    Usage.INFINITE -> Usage.ONCE_OR_MORE
                    Usage.AT_MOST_ONCE -> Usage.UNKNOWN
                    Usage.ONCE_OR_MORE -> Usage.ONCE_OR_MORE
                    Usage.UNKNOWN -> Usage.UNKNOWN
                }
            }
            Usage.UNKNOWN -> Usage.UNKNOWN
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
    BOTTOM, ZERO, ONCE, INFINITE, AT_MOST_ONCE, ONCE_OR_MORE, UNKNOWN
}

class ScopeInformation(val executedAtMostOnce: Boolean, val Parent : ScopeInformation? = null)
{
    val Variables : MutableMap<String,UsageInformation> = mutableMapOf()

    fun print() : String{
        var result = executedAtMostOnce.toString() + "\n"
        Variables.forEach{
            result += it.key + " " + it.value.UsageAmount + "\n"
        }
        if (Parent != null) {
            result = Parent.print() + "sub-scope:" + "\n" + result
        }
        return result
    }
}

class UsageInformation (var UsageAmount : Usage, val name : String, val topScope : Boolean, val Parent : UsageInformation? = null)
{
    val Variables : MutableMap<String, UsageInformation> = mutableMapOf()
}
