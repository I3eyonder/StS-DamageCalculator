package dmgcalculator.entities

data class Range(var min: Int, var max: Int) {

    val value: Int
        get() = max

    val isConstantsValue: Boolean
        get() = min == max

    constructor(value: Int = 0) : this(value, value)

    constructor(other: Range) : this(other.min, other.max)

    fun set(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    fun set(value: Int) {
        this.set(value, value)
    }

    operator fun plus(amount: Int): Range {
        return Range(min + amount, max + amount)
    }

    operator fun plus(other: Range): Range {
        return Range(min + other.min, max + other.max)
    }

    operator fun minus(amount: Int): Range {
        return Range(min - amount, max - amount)
    }

    operator fun minus(other: Range): Range {
        return Range(min - other.max, max - other.min)
    }

}
