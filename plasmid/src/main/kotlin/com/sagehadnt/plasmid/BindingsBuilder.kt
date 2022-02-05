package com.sagehadnt.plasmid

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        Injector.instance = Injector(bindings, defaultSupplier)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(BindingsBuilder::class.java)
    }
}