# Plasmid
Plasmid is a simple dependency injection framework written in Kotlin. It's designed for minimal-fuss, legible injection, without any complex features or signficant overhead.

## Examples

Let's say we have a class that needs access to the filesystem:
```kotlin
class SessionManager {
    val fileLoader: FileLoader = ProductionFileLoader()
    
    fun startSession(filename: String) {
        val sessionSettings = fileLoader.load(filename)
        ...
    }
}
```
This works fine in production, but when we want to test it, we probably don't want to be messing around with real files. We could inject the FileLoader through the constructor, but then we might have to pass it through a long chain of constructors. With Plasmid, we can write:
```kotlin
class SessionManager {
    val fileLoader = inject<FileLoader>()
    ...
}
```
And then all we have to do is configure a binding:
```kotlin
fun main() {
    configureBindings {
        bind<FileLoader> { ProductionFileLoader() }
    }
    SessionManager().startSession("default")
}
```
This tells Plasmid: "Whenever I ask you to `inject<FileLoader>()`, give me the result of the lambda -- i.e. a new `ProductionFileLoader`." This means that when we're writing tests, we can easily specify a mock or alternate binding:

```kotlin
@Before
fun setupTest() {
    configureBindings {
        bind<FileLoader> { mock(FileLoader::class.java) }
    }
}
```
... and everything will just work.

## Caveats
Make sure you always configure a binding before calling inject() for that type, otherwise Plasmid will throw an error.

Bindings are on *exact* types. When you inject, make sure you are parameterised on the bound type and not on some subtype. E.g.:
```kotlin
interface A
interface B : A
class C : B

fun main() {
    configureBindings {
        bind<A> { C() }
    }
    val a = inject<A>() // returns a new instance of C as configured in the binding
    val b = inject<B>() // throws an error because even though there's an implicit binding via A, there's no exact binding via B
    val b: B = inject<A>() // returns a new instance of C as configured in the binding
}
```

## Should I use Plasmid?
Probably not! I built this for my own projects and as coding practice.

If you're looking for a dependency injection framework, I'd suggest https://github.com/InsertKoinIO/koin instead (unaffiliated with this project)
