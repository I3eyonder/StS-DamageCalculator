package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.powers.BufferPower
import com.megacrit.cardcrawl.powers.CurlUpPower
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower
import com.megacrit.cardcrawl.powers.IntangiblePower

data class CreatureInfo(
    val creature: AbstractCreature,
    val remainHP: Int = creature.currentHealth,
    val remainBlock: Int = creature.currentBlock,
    val remainBuffer: Int = creature.getPower(BufferPower.POWER_ID)?.amount ?: 0,
    val hasCurlUpPower: Boolean = creature.hasPower(CurlUpPower.POWER_ID),
    val hasIntangiblePower: Boolean = creature.hasPower(IntangiblePower.POWER_ID),
    val hasIntangiblePlayerPower: Boolean = creature.hasPower(IntangiblePlayerPower.POWER_ID),
)
