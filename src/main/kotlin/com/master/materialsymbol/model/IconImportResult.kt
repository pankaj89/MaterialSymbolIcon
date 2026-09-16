package com.master.materialsymbol.model

data class IconImportResult(
    val success: Boolean,
    val xmlFilePath: String? = null,
    val composeFilePath: String? = null,
    val appendedToAppIcons: Boolean = false,
    val appIconsProperty: String? = null,
    val errorMessage: String? = null
)
