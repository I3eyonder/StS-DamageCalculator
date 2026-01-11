package dmgcalculator.util

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.GdxRuntimeException
import dmgcalculator.DmgCalculatorMod

object TextureLoader {
    private val textures = HashMap<String, Texture?>()

    /**
     * @param filePath - String path to the texture you want to load relative to resources,
     * Example: imagePath("missing.png")
     * @return **com.badlogic.gdx.graphics.Texture** - The texture from the path provided, or a "missing image" texture if it doesn't exist.
     */
    @JvmStatic
    fun getTexture(filePath: String): Texture? {
        return getTexture(filePath, true)
    }

    /**
     * @param filePath - String path to the texture you want to load relative to resources,
     * Example: imagePath("missing.png")
     * @param linear - Whether the image should use a linear or nearest filter for scaling.
     * @return **com.badlogic.gdx.graphics.Texture** - The texture from the path provided, or a "missing image" texture if it doesn't exist.
     */
    fun getTexture(filePath: String, linear: Boolean): Texture? {
        if (textures[filePath] == null) {
            try {
                loadTexture(filePath, linear)
            } catch (e: GdxRuntimeException) {
                DmgCalculatorMod.logger.info("Failed to find texture $filePath", e)
                val missing = getTextureNull(DmgCalculatorMod.imagePath("missing.png"), false)
                if (missing == null) {
                    DmgCalculatorMod.logger.info("missing.png is missing, should be at " + DmgCalculatorMod.imagePath("missing.png"))
                }
                return missing
            }
        }
        var t = textures[filePath]
        if (t != null && t.textureObjectHandle == 0) {
            textures.remove(filePath)
            t = getTexture(filePath, linear)
        }
        return t
    }

    /**
     * @param filePath - String path to the texture you want to load relative to resources,
     * Example: imagePath("missing.png")
     * @return **com.badlogic.gdx.graphics.Texture** - The texture from the path provided, or null if it doesn't exist.
     */
    fun getTextureNull(filePath: String): Texture? {
        return getTextureNull(filePath, true)
    }

    /**
     * @param filePath - String path to the texture you want to load relative to resources,
     * Example: imagePath("missing.png")
     * @param linear - Whether the image should use a linear or nearest filter for scaling.
     * @return **com.badlogic.gdx.graphics.Texture** - The texture from the path provided, or null if it doesn't exist.
     */
    fun getTextureNull(filePath: String, linear: Boolean): Texture? {
        if (!textures.containsKey(filePath)) {
            try {
                loadTexture(filePath, linear)
            } catch (_: GdxRuntimeException) {
                textures[filePath] = null
            }
        }
        var t = textures[filePath]
        if (t != null && t.textureObjectHandle == 0) {
            textures.remove(filePath)
            t = getTextureNull(filePath, linear)
        }
        return t
    }

    @Throws(GdxRuntimeException::class)
    private fun loadTexture(textureString: String, linearFilter: Boolean = false) {
        val texture = Texture(textureString)
        if (linearFilter) {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        } else {
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        DmgCalculatorMod.logger.info("Loaded texture $textureString")
        textures[textureString] = texture
    }
}