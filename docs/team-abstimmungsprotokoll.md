# Einfaches Abstimmungsprotokoll

## Ziel

Wir wollen klären, wie Java-Services und .NET-Services zusammenarbeiten.
Danach sollen Best Case und Worst Case eindeutig testbar sein.

---

## 1. Wer macht was?

- Shop-Service: nimmt Bestellung an und steuert den Ablauf
- Billing-Service: macht Zahlung und Refund
- Warehouse-Service: reserviert, committet und cancelt Lagerbestand
- Invoice-Service: erstellt Rechnung
- Audit-Service: speichert alle wichtigen Schritte

Offen:

- [ ] Ist der Shop-Service wirklich der zentrale Orchestrator?
- [ ] Welche Services schreiben Audit-Events?

---

## 2. Was läuft über REST?

Shop -> Warehouse:

- [ ] `POST /reservations`
- [ ] `POST /reservations/{id}/commit`
- [ ] `POST /reservations/{id}/cancel`

Shop -> Billing:

- [ ] `POST /payments`
- [ ] `POST /payments/{id}/refund`

Services -> Audit:

- [ ] `POST /audit/events`
- [ ] `GET /audit/orders/{correlationId}`

Offen:

- [ ] Welche JSON-Felder brauchen die Requests?
- [ ] Welche Responses bekommen wir zurück?
- [ ] Welche Fehlercodes nutzen wir?

---

## 3. Was läuft über Kafka?

Vorschlag für den Anfang:

- Billing sendet `PaymentSucceeded`
- Invoice hört auf `PaymentSucceeded`
- Invoice erstellt danach die Rechnung

Offen:

- [ ] Bleibt es erstmal nur bei `PaymentSucceeded`?
- [ ] Wie heißt das Kafka-Topic?
- [ ] Wie sieht das Event als JSON aus?

---

## 4. Gemeinsame ID

Jede Bestellung braucht eine `correlationId`.

Vorschlag:

- Shop erzeugt die `correlationId`
- alle Services geben diese ID weiter
- Audit speichert alle Events mit dieser ID

Offen:

- [ ] `correlationId` nur im JSON?
- [ ] oder zusätzlich als HTTP-Header?

---

## 5. Best Case

Erfolgreicher Ablauf:

1. Kunde bestellt.
2. Warehouse reserviert.
3. Billing bezahlt.
4. Invoice erstellt Rechnung.
5. Warehouse committet.
6. Bestellung ist `COMPLETED`.
7. Audit zeigt alle Schritte.

Muss im Test geprüft werden:

- [ ] Bestellung endet mit `COMPLETED`
- [ ] Rechnung wurde erstellt
- [ ] Lager wurde final abgebucht
- [ ] Audit-Timeline ist vollständig
- [ ] überall gleiche `correlationId`

---

## 6. Worst Case

Fehlerfall:

Warehouse-Commit schlägt nach erfolgreicher Zahlung fehl.

Erwarteter Ablauf:

1. Warehouse war reserviert.
2. Zahlung war erfolgreich.
3. Commit im Warehouse schlägt fehl.
4. Billing macht Refund.
5. Bestellung ist `ROLLBACK_COMPLETED`.
6. Audit zeigt Fehler und Refund.

Muss im Test geprüft werden:

- [ ] Bestellung endet mit `ROLLBACK_COMPLETED`
- [ ] Zahlung wurde zurückerstattet
- [ ] Fehler steht im Audit
- [ ] Refund steht im Audit
- [ ] überall gleiche `correlationId`

---

## 7. Was müssen wir heute entscheiden?

- [ ] Wer committet Warehouse: Shop per REST oder Warehouse über Kafka?
- [ ] Audit erstmal per REST oder über Kafka?
- [ ] Welche Events sind Pflicht?
- [ ] Welche Fehlerfälle testen wir?
- [ ] Welche Testdaten nutzen wir?

---

## 8. Ergebnis

Datum:

Teilnehmer:

Entschieden:

1.
2.
3.

Noch offen:

1.
2.
3.
