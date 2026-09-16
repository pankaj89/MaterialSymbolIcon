package com.master.materialsymbol.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "MaterialSymbolIconSettings",
    storages = [Storage("material_symbol_icon.xml")]
)
class IconImporterSettings(val project: Project) : PersistentStateComponent<IconImporterSettings.State> {

    data class State(
        var drawableDestinationPath: String = "",
        var composeDestinationPath: String = "",
        var appIconsFilePath: String = "",
        var autoAppendToAppIcons: Boolean = true,
        var composePackageName: String = "",
        var iconPrefixXml: String = "ic_",
        var iconSuffixCompose: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): IconImporterSettings {
            return project.getService(IconImporterSettings::class.java)
        }
    }
}
