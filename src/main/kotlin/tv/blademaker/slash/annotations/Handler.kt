package tv.blademaker.slash.annotations

import tv.blademaker.slash.BaseSlashCommand

interface Handler {

    val path: String
    val parent: BaseSlashCommand

}