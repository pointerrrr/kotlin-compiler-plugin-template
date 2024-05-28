package foo.bar

import org.itmo.my.pretty.plugin.SomeAnnotation

@SomeAnnotation
fun test() {
    val s = MyClass().foo()
    s.<!UNRESOLVED_REFERENCE!>inc<!>() // should be an error
}

fun <!FUNCTION_WITH_DUMMY_NAME!>dummyTest<!>()
{
    val x = 5
    val a = x + 5
    val b = x + 3
    fun test() : Int
    {
        val z = a * 2
        return z
    }

    while (true)
    {

    }
}

fun dummy2()
{
    if (true)
    {

    }
    else
    {

    }
}