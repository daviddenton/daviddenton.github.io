import _2.LocalFileSystem
import java.io.File
import java.io.FileFilter

object _1 {
    class FileSystem(private val dir: File) {
        fun directories() = dir.listFiles(FileFilter { it.isDirectory })
    }

    val fileSystem: FileSystem = FileSystem(File("."))
    val localDirs = fileSystem.directories()

    fun directories(dir: File) =  dir.listFiles(FileFilter { it.isDirectory })
    val otherLocalDirs = directories(File("."))
}

object _2 {
    interface FileSystem {
        fun directories(): Array<File>
    }

    class LocalFileSystem(private val dir: File) : FileSystem {
        override fun directories() = dir.listFiles(FileFilter { it.isDirectory })
    }

    val localDirs = LocalFileSystem(File(".")).directories()
}

object _3 {
    val fs = LocalFileSystem(File(".")) // to the IDE, fs is a LocalFileSystem
    val localDirs = fs.directories()
}

object _4 {
    interface FileSystem {
        fun directories(): Array<File>
    }

    fun FileSystem(dir: File): FileSystem = object : FileSystem {
        override fun directories() = dir.listFiles(FileFilter { it.isDirectory })
    }

    val fs = FileSystem(File(".")) // to the IDE, fs is a FileSystem
    val localDirs = fs.directories()
}