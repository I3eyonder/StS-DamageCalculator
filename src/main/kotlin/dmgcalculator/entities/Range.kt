package dmgcalculator.entities

data class Range(val min: Int, val max: Int) {

    val value: Int
        get() = max

    val isConstantsValue: Boolean
        get() = min == max

    constructor(value: Int = 0) : this(value, value)

    constructor(other: Range) : this(other.min, other.max)
}

fun Range.sorted(): Range {
    return if (min <= max) {
        this
    } else {
        Range(max, min)
    }
}
