warum Product Copy  in Shop Service

Read Model (Lesemodell) – Aus dem CQRS-Muster (Command Query Responsibility Segregation). Es beschreibt eine spezialisierte Kopie von Daten, die exakt so strukturiert ist, dass ein Service (hier dein Shop/Order-Service) sie blitzschnell lesen kann, ohne andere Services fragen zu müssen.

Data Transfer Object (DTO) / Projection – Im DDD (Domain-Driven Design) nennt man eine solche Entity oft auch eine Projektion der Originaldaten, da sie nur einen Teilbereich des echten Produkts abbildet.


Wohnzimmer
├── Sofas          10 Produkte
├── Couchtische     5 Produkte
├── TV-Möbel        5 Produkte
└── Regale          5 Produkte


Schlafzimmer
├── Betten          8 Produkte
├── Kleiderschränke 4 Produkte
└── Nachttische     3 Produkte


Esszimmer
├── Esstische       4 Produkte
├── Essstühle       3 Produkte
└── Sideboards      3 Produkte


Arbeitszimmer
├── Schreibtische   3 Produkte
├── Bürostühle      3 Produkte
└── Bücherregale    2 Produkte


Outdoor
├── Gartenmöbel     3 Produkte
└── Balkonmöbel     2 Produkte