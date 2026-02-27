package dmgcalculator.entities

import com.megacrit.cardcrawl.core.AbstractCreature
import com.megacrit.cardcrawl.powers.*

data class CreatureInfo(
    val creature: AbstractCreature,
    val remainHP: Int = creature.currentHealth,
    val remainBlock: Int = creature.currentBlock,
    val remainBuffer: Int = creature.getPower(BufferPower.POWER_ID)?.amount ?: 0,
    val hasCurlUpPower: Boolean = creature.hasPower(CurlUpPower.POWER_ID),
    val hasIntangiblePower: Boolean = creature.hasPower(IntangiblePower.POWER_ID),
    val hasIntangiblePlayerPower: Boolean = creature.hasPower(IntangiblePlayerPower.POWER_ID),
    val invincibleAmount: Int = creature.getPower(InvinciblePower.POWER_ID)?.amount ?: -1,
    val malleableAmount: Int = creature.getPower(MalleablePower.POWER_ID)?.amount ?: -1,
)
