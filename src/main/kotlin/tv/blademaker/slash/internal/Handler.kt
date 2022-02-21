package tv.blademaker.slash.internal

import tv.blademaker.slash.BaseSlashCommand

interface Handler {

    val path: String
    val parent: BaseSlashCommand

}