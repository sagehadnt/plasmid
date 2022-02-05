# Plasmid
Plasmid is a simple dependency injection framework written in Kotlin. It's designed for low-fuss and easy-to-understand injection.
All injections are performed at runtime, incurring a very small (but nonzero) overhead.

## Quickstart
Using Plasmid consists of two steps:
- When your program starts, **configure bindings** (i.e. in `main()` for production, or at the start of a test method)
- Whenever you need an instance, **inject** it with `inject<T>()`

### Binding
```kotlin
configureBindings {
    bind<FileLoader> { ProductionFileLoader() }
    // add other bindings
}
```
### Injecting
To inject a default constructor parameter/property:
```kotlin
class App(val fileLoader: FileLoader = inject())
```
To inject a local variable:
```kotlin
fun loadFile(fileName: String) {
    val fileLoader = inject<FileLoader>()
    //...
}
```
To inject a non-constructor property:
```kotlin
class App {
    private val fileLoader = inject<FileLoader>()
    //...
}
```

### Testing
Your bindings will persist across tests, possibly causing tests to interfere with each other. To avoid this:
- **In JUnit 5** -- annotate your test class with `@ExtendWith(PlasmidTestExtension::class)`. Plasmid will automatically unbind after each test.
- **In other test frameworks** -- make sure you call `clearBindings()` after each test, e.g. in an `@AfterEach` teardown function.

You shouldn't ever need to clear bindings in production. The idea is to configure your bindings once at startup and then
never touch them again. Plasmid will throw an error if you try to create new bindings without tearing down the old ones.

## Why would I want this?

Let's say we have a class that needs access to the filesystem:
```kotlin
class SessionManager {
    val fileLoader: FileLoader = ProductionFileLoader()
    
    fun startSession(filename: String) {
        val sessionSettings = fileLoader.load(filename)
        // rest of startup goes here
    }
}
```
This works fine in production, but when we want to test it, we probably don't want to be messing around with real files. We could inject the FileLoader through the constructor, but then we might have to pass it through a long chain of constructors. With Plasmid, we can write:
```kotlin
class SessionManager(val fileLoader: FileLoader = inject()) { /* ... */ }
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