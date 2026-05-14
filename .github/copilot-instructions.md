# Copilot / AI Agent Instructions

Kurzinfo
- Ziel: Schnell produktiv werden im `vse-microservice-online-shop` Repository.
- Fokus: Architekturüberblick, Build/Run-Workflows, Projektkonventionen, Integrationspunkte.

Big Picture (kurz)
- Microservice-Architektur: Kombination aus .NET-Services (Audit, Warehouse, Invoice) und Java-Services (Billing, Shop). Siehe `docker-compose.yml` und `docs/architecture.md`.
- Orchestrierung lokal: `docker compose up --build` (siehe Root-README). .NET-Services sind in `*-service-dotnet` Ordnern, jede Service hat `Dockerfile`, `src/` und `tests/`.
- API-Verträge/OpenAPI: YAMLs liegen in `docs/openapi/` (z. B. `docs/openapi/audit-service.yaml`).

Wesentliche Dateien zum Lesen
- [README.md](README.md) — Startpunkt mit Run-Hinweis.
- [docker-compose.yml](docker-compose.yml) — lokale Orchestrierung, Ports und service-abhängigkeiten.
- [docs/architecture.md](docs/architecture.md) — Systemübersicht, Sequenzen, ADRs.
- `OnlineShop.DotNetServices.sln` — bündelt die .NET-Projekte.

Build & Run (praktisch)
- Schnellstart (lokal, Docker):
  ```bash
  docker compose up --build
  ```
- Einzelnes .NET-Service lokal bauen/starten:
  ```bash
  dotnet restore
  dotnet build ./audit-service-dotnet/src/AuditService
  dotnet run --project ./audit-service-dotnet/src/AuditService
  ```
- Tests: jede Service hat ein `tests/`-Verzeichnis; .NET-Tests mit `dotnet test` ausführen.

Konventionen & Patterns
- Services sind isoliert: Änderungen at einem Service sollten möglichst lokal bleiben und nicht `docker-compose.yml` brechen.
- Audit: Append-only-Strategie, Snapshots per INSERT (siehe `audit-service-dotnet` README und ADRs in `docs/architecture.md`).
- Swagger/HTTP: Viele .NET-Projekte enthalten `SwaggerUi.cs` und `Program.cs` mit top-level host-setup — folge vorhandener Start- und DI-Muster.
- Konfiguration: Environment-Variablen werden über `docker-compose.yml` injiziert (siehe `AUDIT_SERVICE_URL`, `INVOICE_RETRY_COUNT` Beispiele).

Integration & Kommunikation
- Synchron: REST zwischen Shop → Warehouse/Billing (schnelles Feedback benötigt).
- Asynchron: Kafka-events für Hintergrundprozesse (Invoice, final stock commit). Events und Themen sind in `docs/architecture.md` dokumentiert.
- Persistenz: Postgres für Audit (`audit-db` in `docker-compose.yml`).

AI-Änderungsregeln (spezifisch)
- Kleine, orthogonale PRs: Beschränke Änderungen auf ein Service-Verzeichnis pro PR.
- Wenn API-Shape geändert wird: aktualisiere die passende OpenAPI-YAML in `docs/openapi/` und prüfe Konsumenten.
- Vermeide Änderungen an `docker-compose.yml` ohne Testlauf (`docker compose up --build`).
- Bei C#-Änderungen: halte dich an vorhandene `Program.cs`-/`SwaggerUi.cs`-Pattern (kopiere Stil, DI-Registrierungen).

Zum Prüfen vor Commit
- `docker compose up --build` lokal (sanity).
- Für .NET: `dotnet build` + `dotnet test` im jeweiligen Service.
- Bei API-Änderungen: generiere (oder aktualisiere) OpenAPI/YAML und prüfe gegen Konsumenten.

Wo nachfragen, wenn unsicher
- `docs/architecture.md` für das Warum zu Kommunikations- und Persistenzentscheidungen.
- Service-README.md (z. B. `audit-service-dotnet/README.md`) für service-spezifische Details.

Wenn diese Datei bereits vorhanden ist
- Versuche Merge: behalte existierende, wertvolle Hinweise und ergänze nur service-spezifische Fakten aus diesem Repo.

Feedback
- Bitte prüfen und kurz angeben, welche Bereiche ihr detaillierter haben wollt (Build, Tests, Debugging, API-Patterns).
