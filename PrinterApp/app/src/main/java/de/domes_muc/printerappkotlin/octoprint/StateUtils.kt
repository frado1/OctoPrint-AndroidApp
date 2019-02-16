package de.domes_muc.printerappkotlin.octoprint

/**
 * Class with the type of states the printers can hold at any moment
 * @author alberto-baeza
 */
object StateUtils {
    val STATE_ADHOC = -2
    val STATE_NEW = -1
    val STATE_NONE = 0
    val STATE_OPEN_SERIAL = 1
    val STATE_DETECT_SERIAL = 2
    val STATE_DETECT_BAUDRATE = 3
    val STATE_CONNECTING = 4
    val STATE_OPERATIONAL = 5
    val STATE_PRINTING = 6
    val STATE_PAUSED = 7
    val STATE_CLOSED = 8
    val STATE_ERROR = 9
    val STATE_CLOSED_WITH_ERROR = 10
    val STATE_TRANSFERING_FILE = 11

    val SLICER_HIDE = -1
    val SLICER_UPLOAD = 0
    val SLICER_SLICE = 1
    val SLICER_DOWNLOAD = 2

    val TYPE_WITBOX = 1
    val TYPE_PRUSA = 2
    val TYPE_CUSTOM = 3

}
