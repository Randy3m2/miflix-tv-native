package com.miflix.native2.data

enum class BrunioExtensionPermissionRP174 { READ_CATALOG, READ_LIVE, READ_PROFILE, WRITE_QUEUE, WRITE_UI, NETWORK }
data class BrunioExtensionManifestRP174(
    val id: String, val name: String, val version: String,
    val permissions: Set<BrunioExtensionPermissionRP174>, val entryPoint: String,
)
object BrunioExtensionPolicyRP174 {
    fun allowed(manifest: BrunioExtensionManifestRP174): Boolean =
        manifest.id.matches(Regex("[a-z0-9][a-z0-9_.-]{2,63}")) &&
        manifest.version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) &&
        manifest.permissions.size <= 6 && manifest.entryPoint.startsWith("sandbox:")
}
