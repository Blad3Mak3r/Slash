package tv.blademaker.slash

import java.util.*

object SlashInfo {
    private val propsFile = ResourceBundle.getBundle("module")

    val VERSION: String = if (propsFile.getString("version").startsWith("@")) "InDev" else propsFile.getString("version")
}