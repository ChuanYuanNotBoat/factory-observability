# Repository guidance

- Product context is in `README.md`, `docs/architecture.md`, and `docs/roadmap.md`.
- Keep user-facing explanations and project documentation in Chinese; use English Java identifiers.
- Use JDK 21 and the checked-in Gradle Wrapper. Do not commit machine-specific Java paths.
- Keep `core` production code JDK-only. Minecraft, loaders, and optional mods belong outside Core.
- Preserve resource kinds, units, qualifiers, and world/dimension/subject identity in observations and metrics.
- Preserve provenance, quality, coverage, and confidence independently. Unknown values and lost visibility are not zero.
- Inventory deltas are net changes, not evidence of exact production, consumption, or gross transfers.
- Read game objects on the game thread; pass immutable DTOs to analysis workers.
- Reserved modules are not working integrations. Update module status when implementing them.
- Validate relevant changes with `gradlew.bat build` and `gradlew.bat :test-fixtures:run` (or `./gradlew` on Unix).
