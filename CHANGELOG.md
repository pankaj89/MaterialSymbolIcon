<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MaterialSymbolIcons Changelog

## [1.0.2] - 2026-09-18

### Fixed
- Fixed recursive `StackOverflowError` in `AppIcons` by generating import aliases (`import ... as ...Icon`).
- Added automatic repair of existing self-referential getters in `AppIcons.kt`.
- Added GZIP and deflate decompression support in downloader service to reliably fetch Google Fonts `.kt` icon files without corrupting data or triggering unwanted SVG fallback.
- Preserved downloaded Jetpack Compose `.kt` files verbatim with their original variable names (e.g. `encrypted`) and exact vector paths.

## [1.0.1] - 2026-09-16

### Changed
- Increased dialog dimensions (1140x660) and expanded right content panel width (900px) to prevent text wrapping.
- Switched key capabilities layout to BorderLayout for optimal line length and visual alignment.
- Updated official plugin logo and metadata.

## [1.0.0] - 2026-09-16

### Added
- Direct Google Material Symbols import and URL downloading.
- Apple SF Symbols SVG parser with automated proprietary namespace stripping.
- Custom local SVG vector importer with drag-and-drop and live preview.
- One-click dual code generation: Jetpack Compose `ImageVector` and Android Vector Drawable XML.
- Automatic registration to central `AppIcons.kt` registry via Kotlin PSI.
- Dedicated "About Us" developer profile for Pankaj Sharma with portfolio integration.
- Custom vector plugin logo and dark squircle badge.
- Support for Android Studio (Ladybug, Meerkat, Koala) and IntelliJ IDEA 2024.2+.
