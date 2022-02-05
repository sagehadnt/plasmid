package com.sagehadnt.plasmid

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