# Angular-Frontend WohnWerk

## Aufgabe

Das Frontend ist die gemeinsame Oberfläche des Online-Shops. Es zeigt
Kundenfunktionen und interne Ansichten, besitzt aber keine fachlichen Daten.

## Technologie

- Angular 22
- Standalone Components
- Signals
- Guards
- HTTP-Interceptor
- TypeScript 6
- Nginx

## Bereiche der Anwendung

- Shop: Produkte, Warenkorb, Checkout und Bestellstatus
- Kunde: Profil, Adressen und eigene Bestellungen
- Admin: Produkte, Bestellungen, Rechnungen, Audit und Systemstatus
- Warehouse: Bestand und Reservierungen
- Observability: Zugang zu technischen Informationen

Die Routen sind fachlich getrennt. Die Seiten werden über `loadComponent`
erst beim Aufruf der jeweiligen Route geladen.

## API

Nginx leitet relative Pfade weiter:

```text
/api/shop/       -> shop-service
/api/warehouse/  -> warehouse-service
/api/audit/      -> audit-service
/api/invoices/   -> invoice-service
```

## Kafka-Events

Das Frontend kommuniziert nicht direkt mit Kafka. Es liest Ergebnisse über
REST-APIs und die Audit-Timeline.

## Datenbank

Das Frontend besitzt keine fachliche Datenbank. Signals halten nur lokalen
UI-Zustand.

## Konfiguration

Guards schützen Rollenbereiche. Ein HTTP-Interceptor ergänzt Zugangsdaten für
geschützte Admin-Aufrufe. Nginx übernimmt die API-Weiterleitung.

## Docker

Der Docker-Build verwendet Node `22.22.3`. Nginx liefert anschließend die
statischen Angular-Dateien aus.

```bash
docker compose up -d --build frontend-angular
```

Die Anwendung ist unter `http://localhost:3001` erreichbar.

## Tests

Lokal:

```bash
npm install
npm run build
npm test -- --watch=false
```
