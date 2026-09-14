package xyz.savesx2.core

import androidx.compose.runtime.Immutable

/**
 * Encapsulates an active 3D save icon session for interactive 3D rotation and inspection.
 *
 * Holding the pre-parsed [mesh] ensures that binary parsing, RLE decompression,
 * and vertex packaging are executed only once when the viewer opens.
 */
@Immutable
data class Ps2Icon3dSession(
    val title: String,
    val subtitle: String = "",
    val mesh: Ps2IconDecoder.ParsedIconMesh,
    val iconSys: Ps2IconSys? = null
)
