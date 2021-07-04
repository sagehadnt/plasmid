package com.sagehadnt.plasmid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class PlasmidTest {

    @Test
    fun `test production bindings`() {
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
    fun `test test-specific bindings`() {
        configureBindings {
            bind<FileLoader> { TestFileLoader("test-file.txt") }
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(TestFileLoader::class.java)

        val file = fileLoader.load("test-file.txt")
        assertThat(file.name).isEqualTo("test-file.txt")
    }

    @Test
    fun `test using mocks as default bindings`() {
        configureBindings {
            withDefault { Mockito.mock(it) }
        }

        val fileLoader = inject<FileLoader>()
        Mockito.`when`(fileLoader.load("test-file.txt"))
            .thenReturn(File("test-file.txt"))

        val file = fileLoader.load("test-file.txt")
        assertThat(file.name).isEqualTo("test-file.txt")
    }

    @Test
    fun `default should not override specific bindings`() {
        configureBindings {
            bind<FileLoader> { ProductionFileLoader() }
            bindSingleton(FileSystem())
            withDefault { Mockito.mock(it) }
        }
        val fileLoader = inject<FileLoader>()
        assertThat(fileLoader).isInstanceOf(ProductionFileLoader::class.java)

        val file = fileLoader.load("pom.xml")
        assertThat(file.name).isEqualTo("pom.xml")
    }

    @Test
    fun `test nested constructor injection`() {
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

class ProductionFileLoader: FileLoader {

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

class TestFileLoader(vararg files: String): FileLoader {

    private val files: Map<String, File> = files.associateWith { File(it) }

    override fun load(filename: String) = files[filename] ?: error("No such file '$filename'")
}

class FileManager(val fileLoader: FileLoader)