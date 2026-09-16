package com.master.materialsymbol.model

data class IconImportRequest(
    val rawIconName: String,
    val svgContent: String,
    val destinationType: IconDestinationType,
    val customXmlName: String? = null,
    val customComposeName: String? = null,
    val sourceUrl: String? = null
)
