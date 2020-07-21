import java.util.Locale

private val UNSTABLE_KEYWORDS = listOf(
    "alpha",
    "beta",
    "rc",
    "m",
    "eap",
    "dev"
)

fun isUnstable(version: String, currentVersion: String): Boolean {
    val lowerVersion = version.toLowerCase(Locale.ROOT)
    val lowerCurrentVersion = currentVersion.toLowerCase(Locale.ROOT)
    return UNSTABLE_KEYWORDS.any { it in lowerVersion && it !in lowerCurrentVersion }
}

private const val ANDROID = "android"
fun isWrongPlatform(version: String, currentVersion: String): Boolean {
    return ANDROID in currentVersion && ANDROID !in version
}
