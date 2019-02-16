package de.domes_muc.printerappkotlin.octoprint

import java.io.File

class OctoprintFile(val file: File, val p_isDirectory: Boolean) : File(file.toURI())
{
    constructor(pathname: String, p_isDirectory: Boolean) : this(File(pathname), p_isDirectory)
    constructor(parent: String, child: String, p_isDirectory: Boolean): this(File(parent, child), p_isDirectory)
    constructor(parent: File, child: String, p_isDirectory: Boolean) : this(File(parent, child), p_isDirectory)

    private val mIsDirectory = p_isDirectory


    override fun isDirectory(): Boolean {
        return mIsDirectory
    }


}