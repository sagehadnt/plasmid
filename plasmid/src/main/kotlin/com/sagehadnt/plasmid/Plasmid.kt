package com.sagehadnt.plasmid

import org.slf4j.LoggerFactory

/**
 * Obtain a new instance of T using Plasmid's Injector. Before calling this function, make sure you specify
 * a binding for T via Plasmid's configureBindings() function. If there is no explicit or default binding for T,
 * this function will throw an error.
 */
inline fun <reified T> inject(): T {
    return Injector.inject(T::class.java)
}

class Injector(
    val suppliers: Map<Class<*>, () -> Any>,
    val defaultSupplier: ((Class<*>) -> Any)?
) {

    companion object {

        lateinit var instance: Injector

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
    }

}

/**
 * Sets up Plasmid class bindings. After configuration, you can call inject<T>() to create a new instance of type T,
 * provided you have configured a binding for T or a default binding.
 */
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

    inline fun <reified T> bind(crossinline supplier: () -> T) {
        val clazz = T::class.java
        LOGGER.info("Binding $clazz")
        bindings[clazz] = { supplier() as Any }
    }

    inline fun <reified T> bindSingleton(singleton: T) {
        val clazz = T::class.java
        LOGGER.info("Binding $clazz to singleton: $singleton")
        bindings[clazz] = { singleton as Any }
    }

    fun withDefault(defaultSupplier: (Class<*>) -> Any) {
        LOGGER.info("Setting default supplier")
        this.defaultSupplier = defaultSupplier
    }

    fun done() {
        LOGGER.info("Finished configuring bindings")
        Injector.instance = Injector(bindings, defaultSupplier)
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(BindingsBuilder::class.java)
    }

}