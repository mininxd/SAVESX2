package xyz.savesx2.core

enum class BatchExportFormat(
    val extension: String,
    val title: String,
    val description: String
) {
    PSU(
        extension = "psu",
        title = "PSU (.psu)",
        description = "uLaunchELF & EMS Memory Adapter format"
    ),
    MAX(
        extension = "max",
        title = "MAX (.max)",
        description = "Action Replay MAX format"
    ),
    CBS(
        extension = "cbs",
        title = "CBS (.cbs)",
        description = "CodeBreaker format"
    ),
    XPS(
        extension = "xps",
        title = "XPS (.xps)",
        description = "SharkPort / X-Port format"
    )
}
