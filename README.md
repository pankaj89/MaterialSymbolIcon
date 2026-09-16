# MaterialSymbolIcon

A unified developer tool for Android Studio and IntelliJ IDEA to download, convert, and manage icons (Google Material Symbols, Apple SF Symbols, and SVG images) as Jetpack Compose `ImageVector` and Android Vector Drawable XMLs.

---

## Features

- **Google Material Symbols:** Direct URL download, instant vector conversion, live preview, and intelligent auto-naming.
- **Apple SF Symbols:** Paste raw SVG copied from Apple's SF Symbols macOS app with automated stripping of proprietary Apple namespaces (`xmlns:sfs`, `sfs:`) and guide notes.
- **Custom SVG Images:** Browse or drag-and-drop local `.svg` files with real-time vector preview, providing a modern alternative to native Vector Asset Studio.
- **Dual Format Generation:** One-click generation for Jetpack Compose `ImageVector` Kotlin source code, Android Vector Drawable XML, or both simultaneously.
- **Centralized Registry (`AppIcons.kt`):** Automatically appends imported icons to your project's `AppIcons.kt` registry using IntelliJ Kotlin PSI.
- **Smart Path Auto-Detection:** Automatically discovers your project's drawable folders, Compose theme packages, and icon registry files.

---

## How It Works

### 1. Google Material Symbols
1. Go to [Google Fonts Icons](https://fonts.google.com/icons) and select your desired icon.
2. In the right-hand details drawer, choose **Android** and select **Compose**.
3. Copy the URL/code and paste it into the **Symbol URL** field in the plugin.
4. The plugin automatically detects the icon name and generates live previews.
5. Click **Generate ImageVector**, **Generate Drawable**, or **Generate Both**.

### 2. Apple SF Symbols
1. Open Apple's **SF Symbols** application on macOS.
2. Right-click on any icon and select **Copy As > Copy SVG** (or press `Cmd + C`).
3. Paste the SVG snippet into the plugin's **SF Symbols** tab.
4. The plugin automatically cleans Apple-specific SVG attributes and normalizes the `viewBox`.
5. Enter your preferred icon name and click your export target.

### 3. Local SVG Images
1. Open the **SVG Image** tab.
2. Drag and drop any `.svg` file into the drop zone, or browse to select a file from your disk.
3. The plugin parses the SVG, validates the vector paths, and displays an instant preview.
4. Click **Generate ImageVector**, **Generate Drawable**, or **Generate Both**.

### 4. Central Registry Integration
When **Auto-append to AppIcons.kt** is enabled, every imported icon is automatically declared in your central registry file:

```kotlin
// Central registry: AppIcons.kt
object AppIcons {
    val Home: ImageVector
        get() = ...
}
```

You can immediately reference it anywhere in your Compose UI:

```kotlin
Icon(
    imageVector = AppIcons.Home,
    contentDescription = "Home"
)
```

---

## Configuration & Settings

Navigate to the **Settings** tab in the plugin dialog to configure:
* **XML Drawables Path:** Defaults to `app/src/main/res/drawable`.
* **Compose Vectors Path:** Destination folder for Kotlin `ImageVector` files.
* **Compose Package:** Target package declaration for generated Kotlin files.
* **AppIcons.kt Path:** Path to your centralized icon registry.
* **XML Resource Prefix:** Customizable prefix (default: `ic_`).
* **Compose Suffix:** Optional suffix for Compose icon names.
* **Auto-Detect:** Scans your project structure to configure default paths automatically.

---

## Installation

1. In Android Studio or IntelliJ IDEA, go to **Settings / Preferences > Plugins > Marketplace**.
2. Search for **`MaterialSymbolIcon`**.
3. Click **Install** and restart the IDE if prompted.
4. Access the plugin via **Tools > Material Symbol & Icon Importer...** or from the right-hand **MaterialSymbolIcon** tool window.

---

## Author

Created & Maintained by **Pankaj Sharma**  
Website & Portfolio: [https://pankaj89.vercel.app/](https://pankaj89.vercel.app/)
