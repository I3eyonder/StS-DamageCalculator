package dmgcalculator.util

@Suppress("UNCHECKED_CAST")
fun <T> Any.getPrivateField(fieldName: String): T? {
    var clazz: Class<*>? = this::class.java
    while (clazz != null) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            return field.get(this) as? T
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        }
    }
    return null
}
