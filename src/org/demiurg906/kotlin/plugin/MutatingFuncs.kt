/*package org.demiurg906.kotlin.plugin

class MutatingFuncs {
    fun <A, B> List<A>.mapInPlace(shouldMutate: Mutate = Mutate.NO, transform: (A) -> B): List<B> =
        when (shouldMutate) {
            Mutate.NO -> this.map(transform)
            Mutate.YES -> {
                for (i in indices) {
                    this[i] = @Suppress("UNCHECKED_CAST") transform(this[i])
                }
                this as List<B>
            }
        }

    fun <T> Array<T>.mapInPlaceA(transform: (T) -> T): Array<T> {
        for (i in this.indices) {
            this[i] = transform(this[i])
        }
        return this
    }

    fun <A> List<A>.filter(shouldMutate : Mutate = Mutate.NO, filter : (A) -> Boolean): List<A> =
    when (shouldMutate) {
        Mutate.NO -> this.filter(filter)
        Mutate.YES -> {
            var yes = 0
            for (i in 0..<size) {
                if (filter(this[i]))
                    this[yes++] = this[i]
            }
            this
        }
    }

    val a = listOf(1,2,3)
    fun tst()
    {
        a.mapInPlace { it + 1 }
    }

    //val x = list.map(f).map(g)

    //val y = list.map(Mutate.YES, f).map(Mutate.YES, g)

    enum class Mutate {
        YES, NO
    }
}*/