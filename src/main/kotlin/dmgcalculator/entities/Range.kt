package dmgcalculator.entities

class Range {

    var min: Int = 0
        private set

    var max: Int = 0
        private set

    val value: Int
        get() = max

    val isConstantsValue: Boolean
        get() = min == max

    constructor(min: Int, max: Int) {
        set(min, max)
    }

    constructor(value: Int = 0) : this(value, value)

    fun set(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    fun set(value: Int) {
        this.set(value, value)
    }
}
