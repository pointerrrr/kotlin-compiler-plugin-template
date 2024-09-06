package foo.bar

class Something {

    fun dummy2() {
        val blub = listOf("a", "b", "c")
        val asdf: List<String> = blub.mapMutate(Mutate.NO, this::identity)
    }

    fun <A> identity(ret: A): A {
        return ret
    }

    // invariant: if Mutate.YES ==> B : A
    fun <A, B> List<A>.mapMutate(shouldMutate: Mutate = Mutate.NO, transform: (A) -> B): List<B> =
        when {
            shouldMutate == Mutate.YES && this is MutableList<*> -> {
                val me: MutableList<A> = this as MutableList<A>
                val result: MutableList<B> = this as MutableList<B>
                for (i in indices) {
                    result[i] = transform(me[i])
                }
                this
            }

            else -> this.map(transform)
        }

    enum class Mutate { YES, NO }
}

