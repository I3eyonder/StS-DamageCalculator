package dmgcalculator.util

import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun <T> Any.getPrivateField(fieldName: String): T {
    val field: Field = this::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as T
}
