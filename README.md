# VSE Microservice Online Shop

Repository for the VSE online shop project.

## Implemented .NET services

- `audit-service-dotnet`: immutable audit snapshots in PostgreSQL.
- `warehouse-service-dotnet`: stock reservation, cancellation, and commit.
- `invoice-service-dotnet`: invoice creation after `PaymentSucceeded` with retry handling.

All three services target `.NET 10` and are wired in `docker-compose.yml`.

## Architecture

The .NET services follow a pragmatic Clean Architecture layout:

```text
Domain/          business entities and invariants, no framework dependencies
Application/     use cases and ports/interfaces
Infrastructure/  PostgreSQL, HTTP clients, file storage, in-memory adapters
Api/             request models and HTTP-specific helpers
Program.cs       dependency injection and endpoint mapping only
```

Dependencies point inward: `Infrastructure` implements `Application` interfaces, while `Domain` does not know about ASP.NET Core, PostgreSQL, Docker, or Swagger.

System diagrams for the full Java/.NET architecture are documented in [docs/system-diagrams.md](docs/system-diagrams.md).

Diagrams for the .NET services are documented in [docs/dotnet-services-diagrams.md](docs/dotnet-services-diagrams.md).

## Run

Requires Docker and .NET 10-compatible container images.

```bash
docker compose up --build
```

Ports:

- Audit Service: `http://localhost:5081`
- Warehouse Service: `http://localhost:5082`
- Invoice Service: `http://localhost:5083`

## Local SDK note

The projects target `net10.0`. Install the .NET 10 SDK before running `dotnet restore`, `dotnet build`, or `dotnet test` locally.

## Tests

The repository contains focused unit tests for the .NET services:

- Audit snapshot creation and timeline query
- Warehouse reservation, out-of-stock handling, and cancellation
- Invoice creation, retry handling, and payment validation

With a local .NET 10 SDK:

```bash
dotnet test OnlineShop.DotNetServices.sln
```

Without a local .NET 10 SDK, run the tests through Docker:

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace mcr.microsoft.com/dotnet/sdk:10.0 dotnet test OnlineShop.DotNetServices.sln
```
