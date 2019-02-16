package de.domes_muc.printerappkotlin

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object ListContent {

    val ID_DEVICES = "Devices"
    val ID_LIBRARY = "Library"
    val ID_VIEWER = "Viewer"
    val ID_SETTINGS = "Settings"
    val ID_DEVICES_SETTINGS = "Devices_settings"
    val ID_DETAIL = "Detail"
    val ID_PRINTVIEW = "PrintView"
    val ID_INITIAL = "Initial"

    /**
     * A dummy item representing a piece of content.
     */
    class DrawerListItem(var type: String, var model: String, var time: String, var date: String, var path: String) {
        var id: String? = null
        var icon: String? = null

        override fun toString(): String {
            return model
        }
    }

}
