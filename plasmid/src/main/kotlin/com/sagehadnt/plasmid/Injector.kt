package com.sagehadnt.plasmid

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Obtain a new instance of T using Plasmid's Injector. Before calling this function, make sure you specify
 * a binding for T via Plasmid's configureBindings() function. If there is no explicit or default binding for T,
 * this function will throw an error. Bindings must match the exact bound type -- supertypes and subtypes are
 * ignored.
 */
@PlasmidDSL
inline fun <reified T> inject(): T {
    return Injector.inject(T::class.java)
}

/**
 * Handles Plasmid's injections. If you're using Plasmid as a library, you shouldn't need to call any functions in
 * this class! It's only exposed as public to allow us to reify the type parameter on inject<T>() (which requires
 * us to inline the function, meaning this class must be public).
 */
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

