package com.sagehadnt.plasmid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class PlasmidTest {

    @AfterEach
    fun tearDown() {
        clearBindings()
    }

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

        val file = fileLoader.load("pom.xml")
        assertThat(file.name).isEqualTo("pom.xml")
    }

    @Test
    fun `inject fake implementations`() {
        configureBindings {
            bind<FileLoader> { TestFileLoader("test-file.txt") }
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(TestFileLoader::class.java)

        val file = fileLoader.load("test-file.txt")
        assertThat(file.name).isEqualTo("test-file.txt")
    }

    @Test
    fun `inject mocks`() {
        configureBindings {
            bind<FileLoader> {
                mock(FileLoader::class.java).apply {
                    `when`(load("test-file.txt"))
                        .thenReturn(File("test-file.txt"))
                }
            }
        }

        val fileLoader = inject<FileLoader>()
        val file = fileLoader.load("test-file.txt")
        assertThat(file.name).isEqualTo("test-file.txt")
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

        val file = fileLoader.load("pom.xml")
        assertThat(file.name).isEqualTo("pom.xml")
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

        val file = fileLoader.load("pom.xml")
        assertThat(file.name).isEqualTo("pom.xml")
    }
}

data class File(val name: String)

interface FileLoader {
    fun load(filename: String): File
}

class ProductionFileLoader : FileLoader {

    private val fileSystem = inject<FileSystem>()

    override fun load(filename: String) = fileSystem.load(filename)
}

class FileSystem {

    private val files: Map<String, File> = listOf(
        "pom.xml",
        "config",
        "notes.md"
    ).associateWith { File(it) }

    fun load(filename: String): File {
        return files[filename] ?: error("No such file '$filename'")
    }
}

class TestFileLoader(vararg files: String) : FileLoader {

    private val files: Map<String, File> = files.associateWith { File(it) }

    override fun load(filename: String) = files[filename] ?: error("No such file '$filename'")
}

class FileManager(val fileLoader: FileLoader)