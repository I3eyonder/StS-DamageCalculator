package dmgcalculator.config

import basemod.ModLabel
import basemod.ModLabeledToggleButton
import basemod.ModPanel
import basemod.ModToggleButton
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig
import com.megacrit.cardcrawl.core.Settings
import com.megacrit.cardcrawl.helpers.FontHelper
import java.io.IOException
import java.util.*


/**
 * Configuration panel for the Damage Calculator mod.
 * Provides UI controls for users to customize mod behavior.
 */
object ModConfigPanel {
    private const val SHOW_BLOCK_INFO_KEY = "SHOW_BLOCK_INFO_KEY"
    private lateinit var config: SpireConfig

    fun createPanel(): ModPanel {
        val defaults = Properties()
        defaults.setProperty(SHOW_BLOCK_INFO_KEY, "true")
        try {
            config = SpireConfig("DmgCalculator", "config", defaults).apply {
                save()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        ModConfig.showBlockInfo = config.getBool(SHOW_BLOCK_INFO_KEY)
        val panel = ModPanel()
        val toggleBlockInfo = ModLabeledToggleButton(
            "Show block info.",
            400.0f, 700.0f, Settings.CREAM_COLOR, FontHelper.charDescFont,
            ModConfig.showBlockInfo, panel,
            { label: ModLabel -> },
            { button: ModToggleButton ->
                ModConfig.showBlockInfo = button.enabled
                config.setBool(SHOW_BLOCK_INFO_KEY, button.enabled)
                saveConfig()
            })
        panel.addUIElement(toggleBlockInfo)
        return panel
    }

    private fun saveConfig() {
        try {
            config.save()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

