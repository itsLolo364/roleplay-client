# AGENTS — Roleplay Client

Leggi prima **[docs/PROJECT_PRIMER.md](docs/PROJECT_PRIMER.md)**.

## Regole rapide

1. **Stile UI** = launcher (`public/index.html` dark). In-game usa `GlassUi` / `GlassButton`. Non inventare palette diverse.
2. **Niente mipmap** su texture UI (`setFilter(bilinear, false)`). Le mipmap causano cubi bianchi agli angoli.
3. **Sicurezza launcher**: non loggare token; validare path IPC; includere `lib/**` nel build electron-builder.
4. **Mod client-only** Fabric 1.21.8 / Java 21. Package `net.roleplayclient`.
5. Dopo modifiche mod: `gradle build` in `loloclient-mod/`; il launcher deploya il JAR.

Dettagli operativi: [docs/developer-guide.md](docs/developer-guide.md).
