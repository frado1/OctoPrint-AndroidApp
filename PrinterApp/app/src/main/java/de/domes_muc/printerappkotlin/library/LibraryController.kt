package de.domes_muc.printerappkotlin.library

import android.annotation.SuppressLint
import de.domes_muc.printerappkotlin.ListContent
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.model.ModelFile
import de.domes_muc.printerappkotlin.model.ModelPrinter
import de.domes_muc.printerappkotlin.octoprint.StateUtils
import android.database.Cursor
import android.os.Environment
import de.domes_muc.printerappkotlin.octoprint.OctoprintFiles

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import kotlin.experimental.and


/**
 * This class will handle the file storage architecture and retrieval in a static way
 * so every class can access these methods from wherever
 * @author alberto-baeza
 */
@SuppressLint("DefaultLocale")
class LibraryController {
    init {
        fileList.clear()
        //Retrieve normal files
        retrieveFiles(parentFolder, false)

    }

    companion object {

        val fileList = ArrayList<File>()
        /****************************** HISTORY  */

        val historyList = ArrayList<ListContent.DrawerListItem>()
        var currentPath: File? = null
            private set

        val TAB_ALL = "all"
        val TAB_CURRENT = "current"
        val TAB_PRINTER = "printer"
        val TAB_FAVORITES = "favorites"

        /**
         * Retrieve files from the provided path
         * If it's recursive, also search inside folders
         * @param path
         * @param recursive
         */
        fun retrieveFiles(path: File, recursive: Boolean) {

            val files = path.listFiles()

            if (files != null)
                for (file in files) {

                    //If folder
                    if (file.isDirectory) {

                        //exclude files from the temporary folder
                        if (file.absolutePath != parentFolder.toString() + "/temp") {

                            //If project
                            if (isProject(file)) {

                                //Create new project
                                val m = ModelFile(file.absolutePath, "Internal storage")
                                addToList(m)

                                //Normal folder
                            } else {
                                if (recursive) {
                                    //Retrieve files for the folder
                                    retrieveFiles(File(file.absolutePath), true)
                                } else
                                    addToList(file)
                            }
                        }
                        //TODO this will eventually go out
                    } else {
                        //Add only stl and gcode
                        if (hasExtension(0, file.name) || hasExtension(1, file.name)) {
                            addToList(file)
                        }
                    }
                }
            //Set new current path
            currentPath = path
        }

        //Retrieve only files from the individual printers
        fun retrievePrinterFiles(id: Long?, path: File) {

            fileList.clear()
            id?.let {
                val p = DevicesListController.getPrinter(it)
                p?.files?.filter{ it.parent.equals(path.path) }?.forEach { addToList(it) }
            }
//            if (id != null) {
//                val p = DevicesListController.getPrinter(id)
//                p?.files?.forEach { addToList(it) }

//                for (f in p!!.files) {
//                    addToList(f)
//                }
//            }

            //Set the current path pointing to a printer so we can go back
//            currentPath = File("printer/" + id!!)
            currentPath = path
        }


        //Retrieve favorites
        fun retrieveFavorites() {


            for ((_, value) in DatabaseController.getPreferences(DatabaseController.TAG_FAVORITES)) {

                val m = ModelFile(value.toString(), "favorite")
                fileList.add(m)

            }

        }

        //Retrieve main folder or create if doesn't exist
        //TODO: Changed main folder to FILES folder.
        val parentFolder: File
            get() {
                val parentFolder = Environment.getExternalStorageDirectory().toString()
                val mainFolder = File("$parentFolder/PrintManager")
                mainFolder.mkdirs()

                return File(mainFolder.toString())
            }


        //Create a new folder in the current path
        //TODO add dialogs maybe
        fun createFolder(name: String) {

            val newFolder = File(currentPath.toString() + "/" + name)

            if (!newFolder.mkdir()) {

            } else {

                addToList(newFolder)
            }


        }


        //Retrieve a certain element from file storage
        fun retrieveFile(name: String, type: String): String? {

            var result: String? = null
            val folder = File("$name/$type/")
            try {
                val file = folder.listFiles()[0].name
                result = folder.toString() + "/" + file
                //File still in favorites
            } catch (e: Exception) {
                //e.printStackTrace();
            }


            return result

        }

        //Reload the list with another path
        fun reloadFiles(path: String) {

            fileList.clear()

            //Retrieve every single file by recursive search
            //TODO Not retrieving printers

            when ( path ) {
                TAB_ALL -> {
                    val parent = File(parentFolder.toString() + "/Files/")
                    retrieveFiles(parent, true)
                    currentPath = parent
                }

                TAB_PRINTER -> {
                    for (p in DevicesListController.list) {

                        if (p.status != StateUtils.STATE_ADHOC && p.status != StateUtils.STATE_NEW)
                        //we add a printer/ parent to determine inside a printer
                            addToList(File("/printer/${p.id}"))
                    }
                }

                TAB_FAVORITES -> {
                    retrieveFavorites()
                }

                TAB_CURRENT -> {
                    retrieveFiles(currentPath!!, false)
                }

                else -> {
                    if ( path.startsWith("/printer")) {
                        // get printer out of path
                        val printerId = OctoprintFiles.getPrinterIdFromPath(path)
                        LibraryController.retrievePrinterFiles(printerId, File(path))
                    } else {
                        retrieveFiles(File(path), false)

//                        LibraryController.reloadFiles(f.getAbsolutePath())
                        ///any other folder will open normally
//                        retrieveFiles(File(path), false)
                    }

                }
            }
        }

        //Check if a folder is also a project
        //TODO return individual results according to the amount of elements found
        fun isProject(file: File): Boolean {

            val f = FilenameFilter { dir, filename -> filename.endsWith("thumb") }

            if (file.exists()) {
                if (file.list(f).size > 0) return true
            }


            return false
        }

        /**
         * Method to check if a file is a proper .gcode or .stl
         * @param type 0 for .stl 1 for .gcode
         * @param name name of the file
         * @return true if it's the desired type
         */

        fun hasExtension(type: Int, name: String): Boolean {

            when (type) {

                0 -> if (name.toLowerCase().endsWith(".stl")) return true
                1 -> if (name.toLowerCase().endsWith(".gcode") || name.toLowerCase().endsWith(".gco") || name.toLowerCase().endsWith(
                        ".g"
                    )
                ) return true
            }

            return false
        }

        fun addToList(m: File) {
            fileList.add(m)
        }

        fun setCurrentPath(path: String) {
            currentPath = File(path)
        }

        /**
         * Delete files recursively
         * @param file
         */
        fun deleteFiles(file: File) {


            if (file.isDirectory) {

                if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, file.name)) {
                    DatabaseController.handlePreference(DatabaseController.TAG_FAVORITES, file.name, null, false)
                }

                for (f in file.listFiles()) {

                    deleteFiles(f)

                }

            }
            file.delete()


        }

        //Check if a file already exists in the current folder
        fun fileExists(name: String): Boolean {

            for (file in fileList) {

                val nameFinal = name.substring(0, name.lastIndexOf('.'))

                if (nameFinal == file.name) {
                    return true
                }

            }

            return false

        }

        /*
    Calculates a file SHA1 hash
     */
        fun calculateHash(file: File): String {

            var hash = ""

            try {
                val md = MessageDigest.getInstance("SHA1")

                val fis = FileInputStream(file)
                val dataBytes = ByteArray(1024)

                var nread = 0

                while (fis.read(dataBytes)?.let {md.update(dataBytes, 0, it); it} != -1);

                val mdbytes = md.digest()

                //convert the byte to hex format
                val sb = StringBuffer("")
                for (i in mdbytes.indices) {
                    sb.append(Integer.toString((mdbytes[i] ) + 0x100, 16).substring(1))
                }


                hash = sb.toString()

            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return hash

        }

        fun initializeHistoryList() {

            historyList.clear()

            val ch = DatabaseController.retrieveHistory()
            ch.moveToFirst()


            while (!ch.isAfterLast) {

                val item = ListContent.DrawerListItem(
                    ch.getString(3),
                    ch.getString(0),
                    ch.getString(2),
                    ch.getString(4),
                    ch.getString(1)
                )

                addToHistory(item)
                ch.moveToNext()
            }

            DatabaseController.closeDb()
        }

        fun addToHistory(item: ListContent.DrawerListItem) {

            historyList.add(0, item)

        }
    }
}
