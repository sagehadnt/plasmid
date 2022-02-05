package com.sagehadnt.plasmid

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

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

private const val NO_INJECTOR_ERROR =
    "Bindings have not been configured! Call configureBindings() to set up your bindings before calling inject()"
private const val INJECTOR_ALREADY_SET_ERROR =
    "Injector instance has already been set. If you're seeing this error in tests, you need to clear your bindings after each test. Consult the README for how to do this."

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

        /**
         * We want to lock down Injector in production so it can't be accidentally changed later, but need to reserve
         * the right to explicitly reset it so we can clear bindings between tests. We grab the delegate here so
         * we can reset it in clearBindings().
         */
        private val instanceDelegate = LateinitVal<Injector>(INJECTOR_ALREADY_SET_ERROR, NO_INJECTOR_ERROR)
        var instance: Injector by instanceDelegate

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
            if (instanceDelegate.isLocked) {
                instanceDelegate.reset()
                LOGGER.info("Cleared all bindings")
            } else {
                LOGGER.warn("No bindings have been configured")
            }
        }

        private val LOGGER: Logger = LoggerFactory.getLogger(Injector::class.java)
    }
}

/**
 * Like `lateinit var`, but once set it will throw an error on subsequent attempts to change it, protecting
 * against accidental changes when we really want `val`-like behaviour. If necessary, the value can be unlocked again
 * by explicitly calling reset().
 */
class LateinitVal<T>(
    private val alreadySetError: String,
    private val notSetError: String
) {

    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value ?: error(notSetError)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!isLocked) {
            this.value = value
        } else {
            error(alreadySetError)
        }
    }

    val isLocked: Boolean
        get() = value != null

    /**
     * Remove the current value and allow it to be set again.
     */
    fun reset() {
        value = null
    }
}
