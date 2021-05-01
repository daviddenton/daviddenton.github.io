import _2.LocalFileSystem
import java.io.File
import java.io.FileFilter

object _1 {
    class FileSystem(private val dir: File, private val includeHidden: Boolean) {
        fun directories() = dir.listFiles(FileFilter {
            it.isDirectory && (includeHidden || !it.name.startsWith('.'))
        })
        // ... many other methods using the dir
    }

    val fileSystem: FileSystem = FileSystem(File("."), true)
    val localDirs = fileSystem.directories()

    fun directories(dir: File, includeHidden: Boolean) = dir.listFiles(FileFilter {
        it.isDirectory && (includeHidden || !it.name.startsWith('.'))
    })

    val otherLocalDirs = directories(File("."), true)
}

object _2 {
    interface FileSystem {
        fun directories(): Array<File>
    }

    class LocalFileSystem(private val dir: File, private val includeHidden: Boolean) : FileSystem {
        override fun directories() = dir.listFiles(FileFilter {
            it.isDirectory && (includeHidden || !it.name.startsWith('.'))
        })
    }

    val localDirs = LocalFileSystem(File("."), true).directories()
}

object _3 {
    val fs = LocalFileSystem(File("."), true) // to the IDE, fs is a LocalFileSystem
    val localDirs = fs.directories()
}

object _4 {
    interface FileSystem {
        fun directories(): Array<File>
    }

    class LocalFileSystem private constructor(private val dir: File, private val includeHidden: Boolean) : FileSystem {
        override fun directories() = dir.listFiles(FileFilter {
            it.isDirectory && (includeHidden || !it.name.startsWith('.'))
        })

        companion object {
            operator fun invoke(dir: File, includeHidden: Boolean): FileSystem = LocalFileSystem(dir, includeHidden)
        }
    }

    val fs = LocalFileSystem(File("."), true) // to the IDE, fs is now a FileSystem
}

object _5 {
    interface FileSystem {
        fun directories(): Array<File>
    }

    fun FileSystem(dir: File, includeHidden: Boolean): FileSystem = object : FileSystem {
        override fun directories() = dir.listFiles(FileFilter {
            it.isDirectory && (includeHidden || !it.name.startsWith('.'))
        })
    }

    val fs = FileSystem(File("."), true) // to the IDE, fs is a FileSystem
    val localDirs = fs.directories()
}
