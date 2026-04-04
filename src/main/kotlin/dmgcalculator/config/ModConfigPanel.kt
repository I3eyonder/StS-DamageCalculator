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
    private const val CALCULATE_PLAYER_THORNS_DAMAGE = "CALCULATE_PLAYER_THORNS_DAMAGE"

    private lateinit var config: SpireConfig

    fun createPanel(): ModPanel {
        setupConfig()
        ModConfig.showBlockInfo = config.getBool(SHOW_BLOCK_INFO_KEY)
        ModConfig.calculatePlayerThornsDamage = config.getBool(CALCULATE_PLAYER_THORNS_DAMAGE)
        return ModPanel().apply {
            addUIElement(createToggleBlockInfoButton(this))
//            addUIElement(createToggleCalculatePlayerThornsDamageButton(this))
        }
    }

    private fun setupConfig() {
        val defaults = Properties().apply {
            setProperty(SHOW_BLOCK_INFO_KEY, "true")
            setProperty(CALCULATE_PLAYER_THORNS_DAMAGE, "true")
        }
        try {
            config = SpireConfig("DmgCalculator", "config", defaults).apply {
                save()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createToggleBlockInfoButton(panel: ModPanel): ModLabeledToggleButton = ModLabeledToggleButton(
        "Show block info.",
        400.0f, 700.0f, Settings.CREAM_COLOR, FontHelper.charDescFont,
        ModConfig.showBlockInfo, panel,
        { label: ModLabel -> },
        { button: ModToggleButton ->
            ModConfig.showBlockInfo = button.enabled
            config.setBool(SHOW_BLOCK_INFO_KEY, button.enabled)
            saveConfig()
        })

    private fun createToggleCalculatePlayerThornsDamageButton(panel: ModPanel): ModLabeledToggleButton =
        ModLabeledToggleButton(
            "Calculate player thorns damage.",
            400.0f, 650.0f, Settings.CREAM_COLOR, FontHelper.charDescFont,
            ModConfig.calculatePlayerThornsDamage, panel,
            { label: ModLabel -> },
            { button: ModToggleButton ->
                ModConfig.calculatePlayerThornsDamage = button.enabled
                config.setBool(CALCULATE_PLAYER_THORNS_DAMAGE, button.enabled)
                saveConfig()
            })

    private fun saveConfig() {
        try {
            config.save()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

