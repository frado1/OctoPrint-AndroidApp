package de.domes_muc.printerappkotlin.octoprint

import de.domes_muc.printerappkotlin.Log
import de.domes_muc.printerappkotlin.devices.DevicesListController
import de.domes_muc.printerappkotlin.devices.database.DatabaseController
import de.domes_muc.printerappkotlin.devices.discovery.PrintNetworkManager
import de.domes_muc.printerappkotlin.model.ModelPrinter

/**
 * Addresses and static fields for the OctoPrint API connection
 *
 * @author alberto-baeza
 */

object HttpUtils {

    val CUSTOM_PORT = ":5000" //Octoprint server listening port

    /**
     * OctoPrint URLs *
     */

    val URL_FILES = "/api/files" //File operations
    val URL_CONTROL = "/api/job" //Job operations
    val URL_SOCKET = "/sockjs/websocket" //Socket handling
    val URL_CONNECTION = "/api/connection" //Connection handling
    val URL_PRINTHEAD = "/api/printer/printhead" //Send print head commands
    val URL_TOOL = "/api/printer/tool" //Send tool commands
    val URL_BED = "/api/printer/bed" //Send bed commands
    val URL_NETWORK = "/api/plugin/netconnectd" //Network config
    val URL_SLICING = "/api/slicing/cura/profiles"
    val URL_DOWNLOAD_FILES = "/downloads/files/local/"
    val URL_SETTINGS = "/api/settings"
    val URL_AUTHENTICATION = "/apps/auth"
    val URL_PROFILES = "/api/printerprofiles"
    val URL_LOGIN = "/api/login"
    val URL_COMMAND = "/api/printer/command"

    /**
     * External links *
     */

    val URL_THINGIVERSE = "http://www.thingiverse.com/newest"
    val URL_YOUMAGINE = "https://www.youmagine.com/designs"

    //Retrieve current API Key from database
    fun getApiKey(url: String): String? {
        val parsedUrl = url.substring(0, url.indexOf("/", 1))


        var id: String? = null

        for (p in DevicesListController.list) {


            when (p.status) {

                StateUtils.STATE_ADHOC ->

                    if (p.name == PrintNetworkManager.currentNetwork.replace("\"", ""))
                        id = PrintNetworkManager.getNetworkId(p.name)

                else ->

                    if (p.address == parsedUrl) {
                        id = PrintNetworkManager.getNetworkId(p.name)

                        if (!DatabaseController.isPreference(DatabaseController.TAG_KEYS, id))
                            id = PrintNetworkManager.getNetworkId(p.address)
                    }
            }


        }

        if (id != null && DatabaseController.isPreference(DatabaseController.TAG_KEYS, id)) {

            return DatabaseController.getPreference(DatabaseController.TAG_KEYS, id)

        } else {

            Log.i("Connection", (id ?: "<unknown>") + " is not preference")
            return ""
        }

    }
}
