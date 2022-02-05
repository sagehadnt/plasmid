package com.sagehadnt.plasmid

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@DslMarker
annotation class PlasmidDSL

/**
 * Obtain a new instance of T using Plasmid's Injector. Before calling this function, make sure you specify
 * a binding for T via Plasmid's configureBindings() function. If there is no explicit or default binding for T,
 * this function will throw an error.
 */
@PlasmidDSL
inline fun <reified T> inject(): T {
    return Injector.inject(T::class.java)
}

/**
 * Remove all bindings, allowing you to call configureBindings() again. You shouldn't use this in production. Plasmid
 * provides this functionality so you can clean up after your tests -- you must call this function after each test
 * or your bindings will persist across tests! If you're using JUnit 5, you can annotate your test class with
 * "@ExtendWith(PlasmidTestRunner::class)" to do this automatically.
 */
@PlasmidDSL
fun clearBindings() {
    Injector.clearBindings()
}

class Injector(
    val suppliers: Map<Class<*>, () -> Any>,
    val defaultSupplier: ((Class<*>) -> Any)?
) {

    companion object {

        private var instance_: Injector? = null

        val instance: Injector
            get() = instance_
                ?: error("Bindings have not been configured! Call configureBindings() to set up your bindings before calling inject()")

        fun setInstance(instance: Injector) {
            if (this.instance_ == null) {
                this.instance_ = instance
            } else {
                error("Injector instance has already been set")
            }
        }

        inline fun <reified T> inject(clazz: Class<T>): T {
            val newObj = getValue(clazz) ?: getDefault(clazz)
            return newObj?.castTo(clazz)
                ?: error("No type binding available for $clazz. Available: ${instance.suppliers.keys}")
        }

        fun <T> getValue(clazz: Class<T>) = instance.suppliers[clazz]?.invoke()

        fun <T> getDefault(clazz: Class<T>): Any? = try {
            instance.defaultSupplier?.invoke(clazz)
        } catch (e: Exception) {
            error("Cannot inject default value for $clazz due to error: $e")
        }

        inline fun <reified T> Any.castTo(clazz: Class<T>): T {
            return this as? T
                ?: error("Invalid type binding: '$this' is not an instance of $clazz")
        }

        fun clearBindings() {
            if (instance_ != null) {
                instance_ = null
                LOGGER.info("Cleared all bindings")
            } else {
                LOGGER.warn("No bindings have been configured")
            }
        }

        private val LOGGER: Logger = LoggerFactory.getLogger(Injector::class.java)
    }
}

/**
 * Sets up Plasmid class bindings. After configuration, you can call inject<T>() to create a new instance of type T,
 * provided you have configured a binding for T or a default binding.
 */
@PlasmidDSL
fun configureBindings(operation: BindingsBuilder.() -> Unit) {
    BindingsBuilder()
        .apply(operation)
        .done()
}

class BindingsBuilder {

    val bindings: MutableMap<Class<*>, () -> Any> = HashMap()
    private var defaultSupplier: ((Class<*>) -> Any)? = null

    init {
        LOGGER.info("Configuring bindings")
    }

    /**
     * Bind the specified supplier to type T. Later, when you call inject<T>(), Plasmid will invoke the specified
     * supplier. Types are bound by their exact type, so if you call inject() with a supertype or subtype of T,
     * this binding will not be invoked.
     */
    @PlasmidDSL
    inline fun <reified T> bind(crossinline supplier: () -> T) {
        val clazz = T::class.java
        LOGGER.info("Binding $clazz")
        bindings[clazz] = { supplier() as Any }
    }

    /**
     * Bind the specified object to type T. Later, when you call inject<T>(), Plasmid will return the object.
     * Types are bound by their exact type, so if you call inject() with a supertype or subtype of T,
     * this binding will not be invoked.
     */
    @PlasmidDSL
    inline fun <reified T> bindSingleton(singleton: T) {
        val clazz = T::class.java
        LOGGER.info("Binding $clazz to singleton: $singleton")
        bindings[clazz] = { singleton as Any }
    }

    /**
     * Sets the default binding. Later, when you call inject<T>() for a T without a configured binding, Plasmid
     * will call this supplier.
     */
    @PlasmidDSL
    fun withDefault(defaultSupplier: (Class<*>) -> Any) {
        LOGGER.info("Setting default supplier")
        this.defaultSupplier = defaultSupplier
    }

    fun done() {
        LOGGER.info("Finished configuring bindings")
        Injector.setInstance(Injector(bindings, defaultSupplier))
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(BindingsBuilder::class.java)
    }
}
