package dmgcalculator

import basemod.BaseMod
import basemod.interfaces.OnPlayerTurnStartSubscriber
import basemod.interfaces.PostInitializeSubscriber
import basemod.interfaces.PostRenderSubscriber
import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.evacipated.cardcrawl.modthespire.Loader
import com.evacipated.cardcrawl.modthespire.ModInfo
import com.evacipated.cardcrawl.modthespire.Patcher
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer
import dmgcalculator.interfaces.PlayerEndTurnSubscriber
import dmgcalculator.publisher.PlayerEndTurnPublisher
import dmgcalculator.renderer.MonsterRenderer
import dmgcalculator.renderer.PlayerRenderer
import dmgcalculator.util.TextureLoader.getTexture
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*

@SpireInitializer
class DmgCalculatorMod : PostInitializeSubscriber,
    PostRenderSubscriber,
    OnPlayerTurnStartSubscriber,
    PlayerEndTurnSubscriber {

    private var isPlayerTurn: Boolean = false

    init {
        PlayerEndTurnPublisher.subscribe(this)
        BaseMod.subscribe(this) //This will make BaseMod trigger all the subscribers at their appropriate times.
        logger.info("$modID subscribed to DmgCalculationMod.")
    }

    override fun receivePostRender(spriteBatch: SpriteBatch?) {
        spriteBatch?.let {
            PlayerRenderer.render(it, isPlayerTurn)
            MonsterRenderer.render(it)
        }
    }

    override fun receiveOnPlayerTurnStart() {
        isPlayerTurn = true
    }

    override fun onPlayerEndTurn() {
        isPlayerTurn = false
    }

    override fun receivePostInitialize() {
        //This loads the image used as an icon in the in-game mods menu.
        val badgeTexture = getTexture(imagePath("dmg-calculator-badge.png"))

        //Set up the mod information displayed in the in-game mods menu.
        //The information used is taken from your pom.xml file.

        //If you want to set up a config panel, that will be done here.
        //You can find information about this on the BaseMod wiki page "Mod Config and Panel".
        BaseMod.registerModBadge(
            badgeTexture,
            info!!.Name,
            info!!.Authors?.joinToString(", "),
            info!!.Description,
            null
        )
    }

    companion object {

        private val resourcesFolder: String = checkResourcesPath()

        var info: ModInfo? = null
        var modID: String? = null //Edit your pom.xml to change this
        val logger: Logger = LogManager.getLogger(modID) //Used to output to the console.

        init {
            loadModInfo()
        }

        //This will be called by ModTheSpire because of the @SpireInitializer annotation at the top of the class.
        @JvmStatic
        fun initialize() {
            DmgCalculatorMod()
        }

        fun imagePath(file: String?): String {
            return "$resourcesFolder/images/$file"
        }

        /**
         * Checks the expected resources path based on the package name.
         */
        private fun checkResourcesPath(): String {
            var name =
                DmgCalculatorMod::class.java.getName() //getPackage can be iffy with patching, so class name is used instead.
            val separator = name.indexOf('.')
            if (separator > 0) name = name.substring(0, separator)

            val resources: FileHandle = LwjglFileHandle(name, Files.FileType.Internal)

            if (!resources.exists()) {
                throw RuntimeException(
                    "\n\tFailed to find resources folder; expected it to be at  \"resources/" + name + "\"." +
                            " Either make sure the folder under resources has the same name as your mod's package, or change the line\n" +
                            "\t\"private static final String resourcesFolder = checkResourcesPath();\"\n" +
                            "\tat the top of the " + DmgCalculatorMod::class.java.getSimpleName() + " java file."
                )
            }
            if (!resources.child("images").exists()) {
                throw RuntimeException(
                    "\n\tFailed to find the 'images' folder in the mod's 'resources/" + name + "' folder; Make sure the " +
                            "images folder is in the correct location."
                )
            }

            return name
        }

        /**
         * This determines the mod's ID based on information stored by ModTheSpire.
         */
        private fun loadModInfo() {
            val infos = Arrays.stream<ModInfo?>(Loader.MODINFOS).filter { modInfo: ModInfo ->
                val annotationDB = Patcher.annotationDBMap[modInfo.jarURL] ?: return@filter false
                val initializers = annotationDB.getAnnotationIndex()
                    .getOrDefault(SpireInitializer::class.java.getName(), mutableSetOf<String?>())
                initializers.contains(DmgCalculatorMod::class.java.getName())
            }.findFirst()
            if (infos.isPresent) {
                info = infos.get()
                modID = info!!.ID
            } else {
                throw RuntimeException("Failed to determine mod info/ID based on initializer.")
            }
        }
    }
}
