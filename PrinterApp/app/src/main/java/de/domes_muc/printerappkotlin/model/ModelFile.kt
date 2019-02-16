package de.domes_muc.printerappkotlin.model

import de.domes_muc.printerappkotlin.R
import de.domes_muc.printerappkotlin.library.LibraryController
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils

import java.io.File

/**
 * Model class to define a printable element, with a reference to its STL, GCODE and storage.
 *
 * @author alberto-baeza
 */
class ModelFile(
    path: String, //Reference to storage
    val storage: String
) : File(path) {

    //Reference to its original stl
    /**
     * ************
     * GETS
     * *************
     */

    var stl: String? = null
        private set

    //Reference to its gcode list
    //TODO Multiple gcodes!
    var gcodeList: String? = null
        private set

    //Reference to image
    var snapshot: Drawable? = null
        private set

    //TODO: Temporary info path
    val info: String
        get() = "$absolutePath/$name.info"

    init {

        //TODO: Move this to the ModelFile code
        setPathStl(LibraryController.retrieveFile(path, "_stl"))
        setPathGcode(LibraryController.retrieveFile(path, "_gcode"))
        setSnapshot("$path/$name.thumb")

    }

    /**
     * *****************
     * SETS
     * ****************
     */

    fun setPathStl(path: String?) {
        stl = path
    }

    fun setPathGcode(path: String?) {
        gcodeList = path
    }

    fun setSnapshot(path: String) {

        try {
            val ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), 256, 256)

            snapshot = BitmapDrawable(Resources.getSystem(), ThumbImage)
            //mSnapshot = Drawable.createFromPath(path);
        } catch (e: Exception) {
            snapshot = Resources.getSystem().getDrawable(R.drawable.ic_file_gray)
        }

    }

}
