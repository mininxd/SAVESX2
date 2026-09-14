package xyz.savesx2.core

/**
 * Represents the release region for a PlayStation save or game disc.
 * Extracted accurately from PS2 / PS1 save directory prefixes and disc serial codes.
 */
enum class Ps2Region(
    val code: String,
    val displayName: String,
    val badgeColor: Long,
    val badgeTextColor: Long
) {
    US(
        code = "US",
        displayName = "North America (NTSC-U)",
        badgeColor = 0xFF12283E,      // Deep Navy
        badgeTextColor = 0xFF90CAF9   // Soft Blue
    ),
    EU(
        code = "EU",
        displayName = "Europe (PAL)",
        badgeColor = 0xFF143322,      // Deep Forest Green
        badgeTextColor = 0xFFA5D6A7   // Soft Green
    ),
    JP(
        code = "JP",
        displayName = "Japan (NTSC-J)",
        badgeColor = 0xFF3D1621,      // Deep Crimson
        badgeTextColor = 0xFFEF9A9A   // Soft Red
    ),
    ASIA(
        code = "ASIA",
        displayName = "Asia",
        badgeColor = 0xFF3D2C11,      // Deep Amber
        badgeTextColor = 0xFFFFCC80   // Soft Gold
    ),
    KR(
        code = "KR",
        displayName = "Korea (NTSC-K)",
        badgeColor = 0xFF2A193D,      // Deep Violet
        badgeTextColor = 0xFFCE93D8   // Soft Purple
    ),
    CN(
        code = "CN",
        displayName = "China (NTSC-C)",
        badgeColor = 0xFF3D1313,      // Deep Maroon
        badgeTextColor = 0xFFFFAB91   // Soft Orange/Coral
    ),
    SYSTEM(
        code = "SYS",
        displayName = "System / Utility",
        badgeColor = 0xFF263238,      // Deep Slate
        badgeTextColor = 0xFFCFD8DC   // Soft Grey
    ),
    UNKNOWN(
        code = "--",
        displayName = "Custom / Other",
        badgeColor = 0xFF212121,
        badgeTextColor = 0xFF9E9E9E
    );

    companion object {
        /**
         * Detects the game region from the save directory name and title.
         */
        fun detect(directoryName: String, title: String = ""): Ps2Region {
            val d = directoryName.trim().uppercase()
            val t = title.trim().uppercase()

            // 1. System / Homebrew folders
            val sysPrefixes = listOf(
                "SYS-CONF", "FMCB", "FHDB", "BOOT", "OPL",
                "FORTUNA", "OPENTUNA", "PADTEST", "ULE", "WLE",
                "SMS", "GSM", "ESR"
            )
            for (prefix in sysPrefixes) {
                if (d.startsWith(prefix)) return SYSTEM
            }

            // 2. High-specificity disc serial codes
            // Asia (SLAJ, SCAJ, TCPS)
            if (d.contains("SLAJ") || d.contains("SCAJ") || d.contains("TCPS")) return ASIA
            // Korea (SLKA, SCKA)
            if (d.contains("SLKA") || d.contains("SCKA")) return KR
            // China (SLCN, SCCN)
            if (d.contains("SLCN") || d.contains("SCCN")) return CN
            // North America (SLUS, SCUS)
            if (d.contains("SLUS") || d.contains("SCUS")) return US
            // Europe (SLES, SCES, TCES)
            if (d.contains("SLES") || d.contains("SCES") || d.contains("TCES")) return EU
            // Japan (SLPS, SCPS, SLPM, SCPM, PAPX, PBPX)
            if (d.contains("SLPS") || d.contains("SCPS") ||
                d.contains("SLPM") || d.contains("SCPM") ||
                d.contains("PAPX") || d.contains("PBPX")
            ) return JP

            // 3. Standard PlayStation save directory prefix conventions
            // PS2 & PS1 saves typically start with B <Region> <Type> or C <Region>
            if (d.length >= 2) {
                val prefix2 = d.take(2)
                if (prefix2 == "BA" || prefix2 == "CA") return US
                if (prefix2 == "BE" || prefix2 == "CE") return EU
                if (prefix2 == "BI" || prefix2 == "CI") return JP
                if (prefix2 == "BK" || prefix2 == "CK") return KR
                if (prefix2 == "BC" || prefix2 == "CC") return CN
            }

            // 4. Secondary checks on game title if directoryName is ambiguous
            if (t.contains("NTSC-U") || t.contains("USA") || t.contains("(US)")) return US
            if (t.contains("PAL") || t.contains("EUROPE") || t.contains("(EU)")) return EU
            if (t.contains("NTSC-J") || t.contains("JAPAN") || t.contains("(JP)")) return JP
            if (t.contains("KOREA") || t.contains("(KR)")) return KR
            if (t.contains("ASIA")) return ASIA

            return UNKNOWN
        }
    }
}
