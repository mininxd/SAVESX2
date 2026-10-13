package xyz.savesx2.core

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Represents a PS2 Game Save folder on a memory card.
 */
@Immutable
data class Ps2Save(
    val directoryName: String,
    val title: String,
    val subtitle: String = "",
    val sizeInBytes: Long = 0,
    val createdDate: String = "",
    val modifiedDate: String = "",
    val isProtected: Boolean = false,
    val isHidden: Boolean = false,
    val isPocketStation: Boolean = false,
    val isPsx: Boolean = false,
    val dirEntry: Ps2DirectoryEntry,
    val files: List<Ps2SaveFile> = emptyList(),
    val iconSys: Ps2IconSys? = null,
    val iconBitmap: Bitmap? = null,
    val iconImageBitmap: ImageBitmap? = iconBitmap?.asImageBitmap(),
    val gameInfo: Ps2GameInfo? = Ps2SerialDatabase.lookupGame(directoryName),
    val region: Ps2Region = gameInfo?.region ?: Ps2Region.detect(directoryName, title)
) {
    val sizeInKb: Long = (sizeInBytes + 1023) / 1024
    val sizeInMb: Double = sizeInBytes / (1024.0 * 1024.0)

    val canonicalGameTitle: String? = gameInfo?.title

    val displayTitle: String = when {
        canonicalGameTitle != null && (title.isBlank() || title.equals(directoryName, ignoreCase = true) || Ps2SerialDatabase.isRawSerialTitle(title)) -> canonicalGameTitle
        title.isNotBlank() -> title
        canonicalGameTitle != null -> canonicalGameTitle
        else -> directoryName
    }

    val displaySubtitle: String = when {
        subtitle.isNotBlank() -> subtitle
        canonicalGameTitle != null && title.isNotBlank() && !title.equals(canonicalGameTitle, ignoreCase = true) && !title.equals(directoryName, ignoreCase = true) -> title
        else -> ""
    }

    val fullDisplayTitle: String = if (displaySubtitle.isNotBlank()) "$displayTitle • $displaySubtitle" else displayTitle
    val modifiedDateOnly: String = modifiedDate.substringBefore(' ')
    val searchKey: String = "${displayTitle.lowercase()} ${directoryName.lowercase()} ${subtitle.lowercase()} ${canonicalGameTitle?.lowercase() ?: ""} ${gameInfo?.serial?.lowercase() ?: ""} ${region.code.lowercase()} ${region.displayName.lowercase()}"
    val sizeText: String = "$sizeInKb KB"
    val firstInitial: String = (displayTitle.firstOrNull() ?: 'P').uppercase()

    val fileCount: Int get() = files.size
}

/**
 * Represents an individual file stored inside a PS2 save folder.
 */
@Immutable
data class Ps2SaveFile(
    val name: String,
    val sizeInBytes: Long,
    val modifiedDate: String,
    val dirEntry: Ps2DirectoryEntry,
    val data: ByteArray? = null
) {
    val sizeInKb: Long get() = (sizeInBytes + 1023) / 1024

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Ps2SaveFile
        return name == other.name && sizeInBytes == other.sizeInBytes && modifiedDate == other.modifiedDate
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sizeInBytes.hashCode()
        return result
    }
}
