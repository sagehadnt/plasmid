package com.sagehadnt.plasmid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@ExtendWith(PlasmidTestExtension::class)
class PlasmidTestExtensionTest {

    /**
     * The PlasmidTestExtension should automatically clear down the bindings after each test. If it doesn't,
     * configureBindings should throw an error on the second and third tests.
     */
    @RepeatedTest(3)
    fun `bindings should be cleared down between tests`() {
        val fileSystem = FileSystem()
        configureBindings { bindSingleton(fileSystem) }
        assertThat(inject<FileSystem>()).isEqualTo(fileSystem)
    }
}