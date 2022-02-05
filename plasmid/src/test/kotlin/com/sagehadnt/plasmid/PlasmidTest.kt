package com.sagehadnt.plasmid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@ExtendWith(PlasmidTestExtension::class)
class PlasmidTest {

    @Test
    fun `bind suppliers`() {
        configureBindings { bind { FileSystem() } }
        assertThat(inject<FileSystem>()).isExactlyInstanceOf(FileSystem::class.java)
    }

    @Test
    fun `bind singletons`() {
        val fileSystem = FileSystem()
        configureBindings { bindSingleton(fileSystem) }
        assertThat(inject<FileSystem>()).isEqualTo(fileSystem)
    }

    @Test
    fun `bind defaults`() {
        configureBindings { withDefault { FileSystem() } }
        assertThat(inject<FileSystem>()).isExactlyInstanceOf(FileSystem::class.java)
    }

    @Test
    fun `clear bindings`() {
        configureBindings { bindSingleton(FileSystem()) }
        clearBindings()
        assertThrows<Exception> { inject<FileSystem>() }
    }

    @Test
    fun `clear bindings and then rebind`() {
        configureBindings { bindSingleton(FileSystem()) }
        clearBindings()
        val reboundFilesystem = FileSystem()
        configureBindings { bindSingleton(reboundFilesystem) }
        assertThat(inject<FileSystem>()).isEqualTo(reboundFilesystem)
    }

    @Test
    fun `should error when no bindings configured`() {
        assertThrows<Exception> { inject<FileLoader>() }
    }

    @Test
    fun `should error when no binding specified but other bindings exist`() {
        val fileSystem = FileSystem()
        configureBindings { bindSingleton(fileSystem) }
        assertThrows<Exception> { inject<FileLoader>() }
    }

    @Test
    fun `should be able to inject into nested objects`() {
        configureBindings {
            bind<FileLoader> { ProductionFileLoader() }
            bindSingleton(FileSystem())
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(ProductionFileLoader::class.java)

        val file = fileLoader.load(FILENAME)
        assertThat(file.name).isEqualTo(FILENAME)
    }

    @Test
    fun `inject fake implementations`() {
        configureBindings {
            bind<FileLoader> { TestFileLoader(FILENAME) }
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(TestFileLoader::class.java)

        val file = fileLoader.load(FILENAME)
        assertThat(file.name).isEqualTo(FILENAME)
    }

    @Test
    fun `inject mocks`() {
        configureBindings {
            bind<FileLoader> {
                mock(FileLoader::class.java).apply {
                    `when`(load(FILENAME))
                        .thenReturn(File(FILENAME))
                }
            }
        }

        val fileLoader = inject<FileLoader>()
        val file = fileLoader.load(FILENAME)
        assertThat(file.name).isEqualTo(FILENAME)
    }

    @Test
    fun `default should not override specific bindings`() {
        configureBindings {
            bind<FileLoader> { ProductionFileLoader() }
            bindSingleton(FileSystem())
            withDefault { mock(it) }
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(ProductionFileLoader::class.java)

        val file = fileLoader.load(FILENAME)
        assertThat(file.name).isEqualTo(FILENAME)
    }

    @Test
    fun `inject into nested constructors`() {
        configureBindings {
            bind { FileManager(inject()) }
            bindSingleton(FileSystem())
            bind<FileLoader> { ProductionFileLoader() }
        }

        val fileManager = inject<FileManager>()
        val fileLoader = fileManager.fileLoader
        assertThat(fileLoader).isInstanceOf(ProductionFileLoader::class.java)

        val file = fileLoader.load(FILENAME)
        assertThat(file.name).isEqualTo(FILENAME)
    }
}

private const val FILENAME = "pom.xml"