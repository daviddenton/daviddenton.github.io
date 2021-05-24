---
layout: post title: "Refactoring Kotlin type-signatures for fun and profit"
tags: [kotlin, fp, design]
comments: false thumbnail: assets/img/refactoring-type-signatures.jpg
---

In which I derive the core type signatures of the [http4k] from an unexpected source, by using refactoring and FP
techniques more commonly associated with coding.

<hr/>

### TL;DR

> **"You can apply simple refactoring and functional techniques such as substitution and currying to refine function signatures."**

<a title="Magic Conjure Conjurer Wand Cards"
href="https://pixabay.com/users/anncapictures-1564471"><img class="article" alt="Image by AnnCapictures from Pixabay"
src="../../../assets/img/refactoring-type-signatures.jpg"/></a>

<hr/>

On a recent episode of TalkingKotlin exploring [http4k], we discussed with [Hadi Hariri] the origins of http4k had been
inspired by the original ["Your Server as a Function"] (SaaF) paper. I noted that the pattern described within was the
same as the one used in the Java Servlet Filter API and that we'd derived the final [http4k] core types of `HttpHandler`
and `Filter` from it.

Just to double check that I wasn't misremembering, I thought I'd revisit the process and write down the steps we went
through. On looking back, these turned out to be remarkably similar to the steps we'd take when refactoring code, but at
a more conceptual level. I thought it would be fun to revisit this process, mapping the steps we took onto the powerful
refactoring functions that come with IntelliJ.

To revisit, the Java Servlet Filter API defines the following:

```java
public interface FilterChain {
    void doFilter(ServletRequest request, ServletResponse response)
}

public interface Filter {
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
}
```

If we transpose these signatures into Kotlin, we get:

```kotlin
interface FilterChain : (ServletRequest, ServletResponse) -> Unit
interface Filter : (ServletRequest, ServletResponse, FilterChain) -> Unit
```

As [http4k] is a library heavily inspired by Functional Programming, we then want to make our types immutable, whereas
the Servlet types are reliant on mutating the HTTP messages. In order for the functions to remain pure, we should return
a `ServletResponse` from our service instead of `Unit`, so let's use **Change Signature (Ctrl+F6)** to do that:

```kotlin
interface FilterChain : (ServletRequest) -> ServletResponse
interface Filter : (ServletRequest, FilterChain) -> ServletResponse
```

We can then **Change Signature (Ctrl+F6)** again to reorder the parameters of the `Filter`:

```kotlin
interface FilterChain : (ServletRequest) -> ServletResponse
interface Filter : (FilterChain, ServletRequest) -> ServletResponse
```

One thing that is common in more Functional Languages, but that we can't currently do in the IDE (hint hint JetBrains!)
is to apply [currying] to split the `Filter` into a Higher-Level Function (ie. one which returns another function):

```kotlin
interface FilterChain : (ServletRequest) -> ServletResponse
interface Filter : (FilterChain) -> (ServletRequest) -> ServletResponse
```

By doing this we can see that we can make a substitution for `FilterChain` into `Filter` as the type signatures match:

```kotlin
interface FilterChain : (ServletRequest) -> ServletResponse
interface Filter : (FilterChain) -> FilterChain
```

Finally, we can do some Kotlin replacement and **Renaming (Shift+F6)** to get us back to the [http4k] types that we know
and love:

```kotlin
typealias HttpHandler = (Request) -> Response

interface Filter : (HttpHandler) -> HttpHandler
```

[http4k]: https://http4k.org
[Hadi Hariri]:
["Your Server as a Function"]:
[Finagle]:
