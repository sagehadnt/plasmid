package com.sagehadnt.plasmid

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor

/**
 * Annotate your JUnit 5 tests with "@ExtendWith(PlasmidTestRunner::class)" to automatically clear bindings between
 * tests. For other test frameworks, invoke the clearBindings() function directly after each test (e.g. in a teardown
 * hook), otherwise your test bindings will persist and interfere with other tests.
 */
class PlasmidTestExtension: TestInstancePostProcessor {
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        clearBindings()
    }
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