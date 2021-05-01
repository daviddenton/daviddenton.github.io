---
layout: post title: "The value of values: Leveraging Kotlin's value classes"
tags: [kotlin, fp, design]
comments: false thumbnail: assets/img/the-value-of-values.jpg
---

In which explore the abilities that Kotlin's [value classes](https://kotlinlang.org/docs/inline-classes.html) give us
for making our code both friendlier and safer, and how
the [Values4k](https://github.com/fork-handles/forkhandles/tree/trunk/values4k)
library extends these for common use-cases.

<hr/>

### TL;DR

> **"Aggressively type domain values to avoid references to StdLib primitive types in business-logic code. This acts as a force multiplier not only in terms of type-safety but also in self-organising code."**

<a title="Antonio de Pereda - The Gentleman's Dream (El sueño del caballero)"
href="https://pixabay.com/illustrations/antonio-pereda-oil-on-canvas-1544616/"><img class="article" alt="Image by
Chaos07 from Pixabay" src="
../../../assets/img/the-value-of-values.jpg"></a>

<hr/>

As precious knowledge that was handed down to me, I've been vocally espousing the use of
**Tiny-types-aka-Microtypes-aka-Domain-Wrappers** for at least as long as I care to remember. The general idea is to
avoid using standard primitive types, well, pretty much anywhere in the signatures of your codebase where the value
represents a domain concept.

Here's a simple example for a bank transfer function:

```kotlin
fun transferMoneyTo(amount: BigDecimal, sortCode: String, account: String)
```

One problem here is one of type-safety - the `sortCode` and `account` arguments are both of type `String`, meaning that
a developer can accidentally switch these values around and we would not potentially notice until runtime. Maybe not
such a problem with two values, but scale up this issue to a function that takes five `Strings` and we start to see how
mistakes could be easily made.

Historically, the Kotlin language and tooling has give us tools to mitigate this problem somewhat - most
notably [named arguments](https://kotlinlang.org/docs/functions.html#named-arguments) and type-hints in the IDE, but
from my experience these are a fairly mediocre safety-net.

Instead, giving each of these values a simple type-safe class wrapper (aka Tiny-type) will mitigate this problem - let's
make it a data class so we retain equality and friendly `toString()` semantics; this also tidies up our function
signature nicely to be safer:

```kotlin
data class Amount(val value: BigDecimal)
data class SortCode(val value: String)
data class Account(val value: String)

fun transferMoneyTo(amount: Amount, sortCode: SortCode, account: Account)
```

One popular argument against this approach is that creating lots of tiny objects on the heap will lead to
performance/memory impact. For the vast majority of projects this impact (in concert with the amazing abilities of the
JVM to self-tune) is probably negligible - but regardless, JetBrains did a clever thing and introduced the idea
of `Inline` (now `Value`) classes in Kotlin.

These classes give us Tiny-type levels of type-safety at compile time, but can be thought of as user-defined boxed
types (in the same way as standard Kotlin primitives such as `Int` and `Boolean`) at runtime. As of Kotlin 1.5.0, we can
switch out our data class to the newly stable value class equivalent, meaning we can use them with none of the
performance impact:

```kotlin
@JvmInline
value class Amount(val value: BigDecimal)
```

> So. We're all done, right? I'll stick value classes everywhere and everything will be sunshine and strawberries?

Sorry - we're not even close!

Regardless of the mechanic that you use to implement it, there are other advantages of Tiny-types which would be useful;
these have always been there - and not just in Kotlin. Once you have these small types proliferating your codebase, lots
of other things may start becoming easier. Want to see everywhere that references a `SortCode`? **Alt + F7** will find
all usages of it. If you have all the instances available in a method call, a quick **Ctrl + Space** and the IDE will
auto-complete a call to `transferMoneyTo()`.

Let's visit each of these other advantages in turn, so we can see how they can be applied to our code. My good friend
Ivan Sanchez collectively referred to these rules as **"The Tiny Book of Tiny Types"**, but I prefer to think of it as a
set of laws which I can name to show off my command of the English language. After all, you did come here to be both
entertained and informed, no?

### The law of attraction

Tiny-type classes themselves should naturally "attract" other pieces of functionality that may be randomly littering
your codebase. In other languages we would be forced to add these to the host class, but Kotlin takes this one step
further with extension functions which we can add on a more localised basis. Regardless of which we choose, we can then
consistently work at the higher level of abstraction - for example using operator functions to sum two `Amounts`, or to
apply a tax rate to the value by adding function:

```kotlin
@JvmInline
value class Amount(val value: BigDecimal) {
    operator fun plus(that: Amount) = Amount((value + that.value))
}

val eleven = Amount(BigDecimal(5)) + Amount(BigDecimal(10))

fun Amount.taxedAt(percent: Int) = Amount(value * BigDecimal((1 - percent / 100.0)))
val eighty = Amount(BigDecimal(100)).taxedAt(20)
```

In fact - even here we can see that in the second example, the `percent` field is potentially its own Tiny-type that
could be extracted, tested and reused. The pattern here that we're aiming for is to remove entirely language level
primitives from our signatures wherever possible.

### The law of amalgamation

But this still isn't good enough. To explain why, a question:

> What do I get if I add £1 and $1 and ¥1?

In one legendary system from the world of finance, this was a real question and the very scary answer was simply "3".
And our `Amount` class from above suffers from the same issue - we still haven't got enough type-safety because although
we could no longer mix up a [Spot price](https://www.investopedia.com/terms/s/spotprice.asp) and
the [Average Daily Trading Volume](https://www.investopedia.com/terms/a/averagedailytradingvolume.asp), our `Amount` is
only one level higher on the abstraction graph - really it's still just a primitive type to us.

A major problem is that it is not legal to sum amounts of different currencies (at least not in any trivial way). So,
following the same mechanic, we could introduce custom `Dollar`, `Pound` and `Yen` classes, or to introduce a
composite `Money` data class with both `amount` and `currency` fields (because value classes cannot contain more than
one field). The latter is definitely more appropriate here, but the point is to define new primitive entities when
appropriate, and to enrich our domain to really start to work in terms of types which other members of our business can
relate.

```kotlin
data class Money(val amount: Amount, val currency: Currency) {
    operator fun plus(that: Money): Money {
        require(currency == that.currency)
        return Money(amount + that.amount, currency)
    }
}
```

Once again - with either solution, our treatment of our values remains the same - we try to raise the abstraction levels
at which we are working to compose more and more sophisticated behaviours.

### The law of propriety

The type-safety will get us so far, but we also want to be able to control construction of our values to ensure that
they are valid. Our `Amount` class takes a `BigDecimal`, but the range of valid values is restricted - at the very least
that it is positive. Our transformation looks like this:

```BigDecimal -> (validate) -> Amount```

We hope that there would be a rule somewhere to prevent someone transferring a negative amount to the target `Account`,
but if we can localise this logic to the `Amount` class construction then we can ensure that an invalid amount is never
processed by our system. Let's add a rule to make sure that we can't construct a negative amount:

```kotlin
@JvmInline
value class Amount(val value: BigDecimal) {
    init {
        require(value > BigDecimal.ZERO)
    }
}
```

That this is trivially testable is a major benefit, but it comes with it's own limitations - we are relying here on
exceptions as the only available failure mechanism (as opposed to a functional `Result` type) and we cannot sanitise the
value on the way in - for example to limit the amount to a certain precision. Luckily there is a workaround which will
give us both of those things - privatising the constructor in concert with a factory function:

```kotlin
@JvmInline
value class Amount private constructor(val value: BigDecimal) {
    companion object {
        fun of(value: BigDecimal): Result<Amount> =
            if (value > ZERO) Result.success(Amount(value.round(MathContext(3))))
            else Result.failure(IllegalArgumentException("less than zero"))
    }
}

val onePointTwoSeven: Result<Amount> = Amount.of(BigDecimal("1.271"))
```

### The law of literacy

Once we know that we are protected from processing invalid values in the guts of our system, we can turn one other
source of potential error - namely the representation of the data entering and exiting our system from external sources,
be they databases or via remote APIs. This mirrors the validation case from above but adds another step to our
transformation:

```String -> (parse) -> BigDecimal -> (validate) -> Amount```

Let's say we've got a special format which prints a special '!' suffix for our amounts. Ingestion can be done with a
companion factory function `parse()` that will handle this conversion for us, returning our choice of exception or
`Result` type.

```kotlin
@JvmInline
value class Amount private constructor(val value: BigDecimal) {
    companion object {
        fun of(value: BigDecimal): Result<Amount> = ...

        fun parse(value: String) = runCatching { of(BigDecimal(value.dropLast(1))).getOrThrow() }
    }
}

val onePointTwoSeven: Result<Amount> = Amount.parse("1.271!")
```

Conversely, we also want to be able to display (or "show") the wrapped value in the correct format. Overriding the

```kotlin
@JvmInline
value class Amount private constructor(val value: BigDecimal) {
    companion object {
        fun of(value: BigDecimal): Result<Amount> = ...

        fun show(amount: Amount) = "${amount.value}!"
    }
}

val amount = Amount.of(BigDecimal("1.267")).getOrThrow()
val onePointTwoSeven = Amount.show(amount)
```

As with most good ideas, and as I'm a (or at least one of a merry band of) horrifically lazy programmer, the concepts
behind Tiny-types were encoded in an open-source library that we could use over and over again without having to
reinvent the wheel at every company we worked at.

### About values4k

We have

The values4k version give us approximately the same thing but has the facility to provide custom handling on top for
either single or multiple values:

```kotlin
@JvmInline
value class Amount private constructor(override val value: BigDecimal) : Value<BigDecimal> {
    companion object : BigDecimalValueFactory<Amount>(
        { Amount(it.round(MathContext(3))) }, BigDecimal("0.01").minValue
    )
}

val onePointTwoSeven: Result<Amount> = Amount.ofResult(BigDecimal("1.271"))
val values: Result<List<Amount>> = Amount.ofListResult(BigDecimal("1.271"), BigDecimal("2.567"))
```
