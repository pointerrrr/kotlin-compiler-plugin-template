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

/*fun dummy2()
{
    if (true)
    {
        val x = 5
    }
    else
    {
        val y = 5
    }
}*/

fun dummy3()
{
    val x = 5
    val z = x + 3
}

import java.math.BigInteger


fun dummyMain(args : Array<String>) //{args: ⊥} -- never used, so omitted for convenience reasons
{
    var m = readln().toBigInteger() // {m: ⊥}
    var p = readln().toBigInteger() // {m: ⊥, p: ⊥}
    var q = readln().toBigInteger() // {m: ⊥, p: ⊥, q: ⊥}
    var a = readln().toBigInteger() // {m: ⊥, p: ⊥, q: ⊥, a: ⊥}
    var wp : BigInteger // {m: ⊥, p: ⊥, q: ⊥, a: ⊥, wp: ⊥}
    var wq : BigInteger // {m: ⊥, p: ⊥, q: ⊥, a: ⊥, wp: ⊥, wq: ⊥}
    if ( q > p ) // {m: ⊥, p: 1, q: 1, a: ⊥, wp: ⊥, wq: ⊥}
    {
        val temp = p // {m: ⊥, p: ∞, q: 1, a: ⊥, wp: ⊥, wq: ⊥}.{temp: ⊥}
        p = q // {m: ⊥, p: 1, q: ∞, a: ⊥, wp: ⊥, wq: ⊥}.{temp: ⊥}
        q = temp // {m: ⊥, p: ∞, q: ∞, a: ⊥, wp: ⊥, wq: ⊥}.{temp: 1}
    }// {m: ⊥, p: [1,∞], q: [1,∞], a: ⊥, wp: ⊥, wq: ⊥}

    val eer = EE(p, q) // {m: ⊥, p: ∞, q: ∞, a: ⊥, wp: ⊥, wq: ⊥, eer: ⊥} -- EE uses p and q exactly once, so add that to both values, which both become ∞
    wp = eer.b * q // {m: ⊥, p: ∞, q: ∞, a: ⊥, wp: ⊥, wq: ⊥, eer: 1, eer.b: 1}
    wq = eer.a * p // {m: ⊥, p: ∞, q: ∞, a: ⊥, wp: ⊥, wq: ⊥, eer: ∞, eer.b: 1, eer.a: 1}

    if (wp < 0.toBigInteger()) // {m: ⊥, p: ∞, q: ∞, a: ⊥, wp: 1, wq: ⊥, eer: ∞, eer.b: 1, eer.a: 1}
        wp = wp + m // {m: 1, p: ∞, q: ∞, a: ⊥, wp: ∞, wq: ⊥, eer: ∞, eer.b: 1, eer.a: 1}
    // {m: [0,1], p: ∞, q: ∞, a: ⊥, wp: [1,∞], wq: ⊥, eer: ∞, eer.b: 1, eer.a: 1}
    if (wq < 0.toBigInteger()) // {m: [0,1], p: ∞, q: ∞, a: ⊥, wp: [1,∞], wq: 1, eer: ∞, eer.b: 1, eer.a: 1}
        wq = wq + m // {m: [1,∞], p: ∞, q: ∞, a: ⊥, wp: [1,∞], wq: ∞, eer: ∞, eer.b: 1, eer.a: 1}
    // {m: ⊤, p: ∞, q: ∞, a: ⊥, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1}

    val grondtallen = EEresult(a % p, a % q) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ⊥}
    // -- a, p, and q are stored in temporary variables before being passed to EEresult, so do not merge with usage information from function header
    val pmacht = (p + 1.toBigInteger()) / 4.toBigInteger() // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ⊥, pmacht: ⊥}
    val qmacht = (q + 1.toBigInteger()) / 4.toBigInteger() // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ⊥, pmacht: ⊥, qmacht: ⊥}

    val ires1 = IndischMachtsverheffen(grondtallen.a, pmacht, p) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: 1, grondtallen.a: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊥}
    val ires2 = IndischMachtsverheffen(grondtallen.b, qmacht, q) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊥, ires2: ⊥}

    val r1 = EEresult(ires1, ires2) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ⊥}
    val r2 = EEresult(ires1, m - ires2) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ⊥, r2: ⊥}
    val r3 = EEresult(m - ires1, ires2) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ⊥, r2: ⊥, r3: ⊥}
    val r4 = EEresult(m - ires1, m - ires2) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: [1,∞], wq: [1,∞], eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ⊥, r2:⊥, r3: ⊥, r4: ⊥}


    val ans1 = (r1.a * wp + r1.b * wq) % m // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ⊥, r3: ⊥, r4: ⊥, ans1: ⊥} -- r1 is used twice, once by each access of its fields, so its usage goes to ∞
    val ans2 = (r2.a * wp + r2.b * wq) % m // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ∞{.a:1, .b: 2}, r3: ⊥, r4: ⊥, ans1: ⊥, ans2: ⊥}
    val ans3 = (r3.a * wp + r3.b * wq) % m // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ∞{.a:1, .b: 2}, r3: ∞{.a: 1, .b:1}, r4: ⊥, ans1: ⊥, ans2: ⊥, ans3: ⊥}
    val ans4 = (r4.a * wp + r4.b * wq) % m // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ∞{.a:1, .b: 2}, r3: ∞{.a: 1, .b:1}, r4: ∞{.a:1, .b: 1, ans1: ⊥, ans2: ⊥, ans3: ⊥, ans4: ⊥}

    val answers = arrayOf(ans1, ans2, ans3, ans4) // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ∞{.a:1, .b: 2}, r3: ∞{.a: 1, .b:1}, r4: ∞{.a:1, .b: 1, ans1: ⊥, ans2: ⊥, ans3: ⊥, ans4: ⊥, answers: ⊥}

    answers.sort() // {m: ⊤, p: ∞, q: ∞, a: ∞, wp: ∞, wq: ∞, eer: ∞, eer.b: 1, eer.a: 1, grondtallen: ∞, grondtallen.a: 1, grondtallen.b: 1, pmacht: ⊥, qmacht: ⊥, ires1: ⊤, ires2: ⊤, r1: ∞{.a: 1,.b:1}, r2: ∞{.a:1, .b: 2}, r3: ∞{.a: 1, .b:1}, r4: ∞{.a:1, .b: 1, ans1: ⊥, ans2: ⊥, ans3: ⊥, ans4: ⊥, answers: 1}

    for (i in 0..3)
        println(answers[i])
    readln()
}

// adapted from https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm
fun EE(p : BigInteger, q : BigInteger) : EEresult //{p: ⊥, q: ⊥}
{
    var s = 0.toBigInteger() // {s: ⊥}, {p: ⊥, q: ⊥}
    var t = 1.toBigInteger() // {s: ⊥, t: ⊥}, {p: ⊥, q: ⊥}
    var r = q // {s: ⊥, t: ⊥, r: ⊥}, {p: ⊥, q: 1}
    var old_s = 1.toBigInteger() // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥}, {p: ⊥, q: 1}
    var old_t = 0.toBigInteger() // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥, old_t: ⊥}, {p: ⊥, q: 1}
    var old_r = p // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥, old_t: ⊥, old_r: ⊥}, {p: 1, q: 1}
    var old_old_r : BigInteger // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥, old_t: ⊥, old_r: ⊥, old_old_r: ⊥}, {p: 1, q: 1}
    var old_old_s : BigInteger // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥, old_t: ⊥, old_r: ⊥, old_old_r: ⊥, old_old_s: ⊥}, {p: 1, q: 1}
    var old_old_t : BigInteger // {s: ⊥, t: ⊥, r: ⊥, old_s: ⊥, old_t: ⊥, old_r: ⊥, old_old_r: ⊥, old_old_s: ⊥, old_old_t: ⊥}, {p: 1, q: 1}
    while (r != 0.toBigInteger()) // {s: ⊥, t: ⊥, r: 1, old_s: ⊥, old_t: ⊥, old_r: ⊥, old_old_r: ⊥, old_old_s: ⊥, old_old_t: ⊥}, {p: 1, q: 1}
    {
        val quotient = old_r / r // {s: ⊥, t: ⊥, r: 1, old_s: ⊥, old_t: ⊥, old_r: 1, old_old_r: ⊥, old_old_s: ⊥, old_old_t: ⊥}.{quotient: ⊥}, {p: 1, q: 1}
        old_old_r = old_r // {s: ⊥, t: ⊥, r: 1, old_s: ⊥, old_t: ⊥, old_r: ∞, old_old_r: ⊥, old_old_s: ⊥, old_old_t: ⊥}.{quotient: ⊥}, {p: 1, q: 1}
        old_r = r // {s: ⊥, t: ⊥, r: ∞, old_s: ⊥, old_t: ⊥, old_r: ∞, old_old_r: ⊥, old_old_s: ⊥, old_old_t: ⊥}.{quotient: ⊥}, {p: 1, q: 1}
        r = old_old_r - quotient * r // {s: ⊥, t: ⊥, r: ∞, old_s: ⊥, old_t: ⊥, old_r: ∞, old_old_r: 1, old_old_s: ⊥, old_old_t: ⊥}.{quotient: 1}, {p: 1, q: 1}
        old_old_s = old_s // {s: ⊥, t: ⊥, r: ∞, old_s: 1, old_t: ⊥, old_r: ∞, old_old_r: 1, old_old_s: ⊥, old_old_t: ⊥}.{quotient: 1}, {p: 1, q: 1}
        old_s = s // {s: 1, t: ⊥, r: ∞, old_s: 1, old_t: ⊥, old_r: ∞, old_old_r: 1, old_old_s: ⊥, old_old_t: ⊥}.{quotient: 1}, {p: 1, q: 1}
        s = old_old_s - quotient * s // {s: ∞, t: ⊥, r: ∞, old_s: 1, old_t: ⊥, old_r: ∞, old_old_r: 1, old_old_s: 1, old_old_t: ⊥}.{quotient: ∞}, {p: 1, q: 1}
        old_old_t = old_t // {s: ∞, t: ⊥, r: ∞, old_s: 1, old_t: 1, old_r: ∞, old_old_r: 1, old_old_s: 1, old_old_t: ⊥}.{quotient: ∞}, {p: 1, q: 1}
        old_t = t // {s: ∞, t: 1, r: ∞, old_s: 1, old_t: 1, old_r: ∞, old_old_r: 1, old_old_s: 1, old_old_t: ⊥}.{quotient: ∞}, {p: 1, q: 1}
        t = old_old_t - quotient * t // {s: ∞, t: ∞, r: ∞, old_s: 1, old_t: 1, old_r: ∞, old_old_r: 1, old_old_s: 1, old_old_t: 1},{quotient: ∞}, {p: 1, q: 1}
    } // {s: ⊤, t: ⊤, r: ⊤, old_s: ⊤, old_t: ⊤, old_r: ⊤, old_old_r: ⊤, old_old_s: ⊤, old_old_t: ⊤}, {p: 1, q: 1}

    return EEresult(old_s, old_t) // {s: ⊤, t: ⊤, r: ⊤, old_s: ⊤, old_t: ⊤, old_r: ⊤, old_old_r: ⊤, old_old_s: ⊤, old_old_t: ⊤}, {p: 1, q: 1}
}


fun IndischMachtsverheffen(grondtal : BigInteger, macht : BigInteger, modulo : BigInteger) : BigInteger {//{grondtal: ⊥, macht: ⊥, modulo: ⊥}
    var res = 1.toBigInteger() // {res: ⊥ }, {grondtal: ⊥, macht: ⊥, modulo: ⊥}
    var grondtalL = grondtal % modulo // {grondtalL: ⊥ , res: ⊥}, {grondtal: 1, modulo: 1, macht: ⊥}
    var machtL = macht // {machtL: ⊥, grondtalL: ⊥, res: ⊥}, {grondtal: 1, modulo: 1, macht: 1}

    while (machtL > 0.toBigInteger()) { //{machtL: 1, grondtalL: ⊥, res: ⊥}, {grondtal: 1, modulo: 1, macht: 1}
        if (((machtL shr 1) shl 1) != machtL) //{machtL: ∞, grondtalL: ⊥, res: ⊥}, {grondtal: 1, modulo: 1, macht: 1}
            res = (res * grondtalL) % modulo //{machtL: ∞, grondtalL: 1, res: 1}, {grondtal: 1, modulo: ∞, macht: 1}

        machtL = machtL shr 1 //{machtL: ∞, grondtalL: 1, res: 1}, {grondtal: 1, modulo: ∞, macht: 1}
        grondtalL = (grondtalL * grondtalL) % modulo //{machtL: ∞, grondtalL: ∞, res: 1}, {grondtal: 1, modulo: ∞, macht: 1}
    } //{machtL: ⊤, grondtalL: ⊤, res: ⊤}, {grondtal: 1, modulo: ⊤, macht: 1} -- machtL, grondtalL, res, modulo
    // are different from before the loop, so we must set them to ⊤

    return res //{machtL: ⊤, grondtalL: ⊤, res: ⊤}, {grondtal: 1, modulo: ⊤, macht: 1}
} //{grondtal: 1, modulo: ⊤, macht: 1} function scope has ended, this is the information about the parameters

class EEresult (var a: BigInteger, var b: BigInteger) // {a: ⊤, b: ⊤} a and b's value are stored and can be accessed from outer scope, so put their usage to ⊤

