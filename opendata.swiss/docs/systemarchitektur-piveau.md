# piveau

## Kurzbeschreibung

Das Subsystem piveau bildet die Metadaten-Verarbeitungsplattform des Systems: Harvesting der Metadaten aus den Quellsystemen, Harmonisierung und persistente Ablage im Triplestore, Indexierung für die Suche sowie automatisierte Bewertung der Metadatenqualität. piveau ist eine Open-Source-Plattform von Fraunhofer FOKUS und besteht aus eigenständigen, containerisierten Microservices, die über das Pipe-Konzept zu Verarbeitungsketten orchestriert werden.

Die Funktionsweise der einzelnen Standardmodule wird hier nicht wiederholt. Massgebend ist die offizielle Dokumentation:

- Dokumentation: [doc.piveau.io](https://doc.piveau.io)
- Quellcode: [gitlab.com/piveau](https://gitlab.com/piveau)

Dieses Kapitel beschränkt sich auf die Abweichungen des ODSn-Setups gegenüber dem piveau-Standard.

## Abbildung

**Abb. 2 – Subsystem piveau: Komponenten und Datenfluss**

_(ArchiMate-View «piveau», Export: `opendata.swiss/archimate/export/piveau.png`)_

## Eingesetzte Komponenten und Abweichungen

| Komponente | Version | Art | Aufgabe | Abweichung |
| --- | --- | --- | --- | --- |
| `piveau-hub-repo` | 4.1.1 | Angepasst | Nimmt die harmonisierten Metadaten entgegen und verwaltet sie im Triplestore | Betrieb gegen GraphDB als Triplestore; ODSn-eigenes piveau-Profil (`opendata-swiss`) mit DCAT-AP-CH-Shapes; Basis-URI `https://opendata.swiss/`; Validator löst die ODSn-eigene System-Pipe `metrics` aus |
| `piveau-hub-search` | 5.3.9 | Angepasst | Pflegt den Suchindex und bedient die Suchanfragen des Frontends | ODSn-spezifische Facetten- und Index-Konfiguration (u. a. Catalogue, Publisher, Organization, Classification); Vokabular-Ersetzungen für `ch-licenses`, `showcase-types` und `legal-forms`; Gazetteer nicht genutzt |
| `piveau-consus-scheduling` | 4.1.0 | Angepasst | Startet die Harvester gemäss dem hinterlegten Zeitplan | Trigger-Definitionen werden im Repository versioniert (`piveau_triggers/bulk.json`) und mit dem ODSn-eigenen Werkzeug `meta_harvester` generiert |
| `piveau-consus-importing-rdf` | 1.10.2 | Standard | Holt die Metadaten von RDF-basierten Quellsystemen ab | – |
| `piveau-consus-exporting-hub` | 8.0.2 | Standard | Übergibt die verarbeiteten Metadaten am Ende der Pipe an den Hub | – |
| `piveau-metrics-validating-shacl` | 4.4.4 | Standard | Prüft die Metadaten gegen die hinterlegten SHACL-Shapes | – |
| `piveau-metrics-annotator` | 2.1.5 | Standard | Ergänzt die Datasets um die Qualitätsmessungen nach DQV | – |
| `piveau-metrics-accessibility` | 2.1.9 | Standard | Prüft die Erreichbarkeit der in den Metadaten referenzierten URLs | – |
| `piveau-metrics-score` | 3.2.5 | Standard | Berechnet aus den Messungen den Qualitäts-Score je Dataset und Katalog | – |
| `piveau-metrics-cache` | 5.8.3 | Angepasst | Hält die berechneten Qualitätswerte für die Auslieferung bereit | Konfiguriert gegen GraphDB und die ODSn-Basis-URI |
| `piveau-consus-importing-csw` | – | Eigenentwicklung | Holt die Geometadaten von geocat.ch über CSW ab | – |
| `piveau-consus-filter` | – | Eigenentwicklung | Prüft Lizenzen und filtert Datasets nach ODSn-Regeln | – |
| `piveau-consus-importing-showcases` | – | Eigenentwicklung | Holt die Showcases aus dem Subsystem UI ab | – |

## Weitere Abweichungen gegenüber dem piveau-Standard

**Metadatenprofil:** Anstelle des generischen DCAT-AP wird ein ODSn-eigenes piveau-Profil eingesetzt (`piveau_profile/`), bestehend aus den SHACL-Shapes für DCAT-AP CH (`dcat-ap-ch.hub.shapes.ttl`), Organisationen (`organization.ttl`) und Showcases (`showcase.hub.shapes.ttl`). Showcases sind als zusätzlicher, im Katalog geführter Ressourcentyp modelliert; dieser existiert im piveau-Standard nicht.

**Eigene Vokabulare:** Zusätzlich zu den piveau-Standardvokabularen werden ODSn-eigene Vokabulare geführt (Schweizer Lizenzen, Showcase-Typen, Rechtsformen aus I14Y).

**Konfiguration als Code:** Pipes, Kataloge, Organisationen, Vokabulare und Trigger sind vollständig im Repository versioniert und werden über GitHub Actions in die Umgebungen ausgerollt. Die Harvester-Definitionen liegen als je eine Pipe-Definition pro Katalog vor (rund 140 Pipes für Bund, Kantone, Städte, geocat.ch, I14Y und LINDAS).

**Qualitäts-Pipe:** Die System-Pipe `metrics` ist ODSn-spezifisch definiert und verkettet SHACL-Validierung, Annotation, Accessibility-Prüfung, Scoring und Rückschreiben in den Hub.

**Benutzeroberfläche:** Anstelle von `piveau-hub-ui` wird eine ODSn-eigene Oberfläche eingesetzt (siehe Kapitel 1.2.2).

Es werden keine piveau-Standardkomponenten geforkt oder im Code verändert; Abweichungen erfolgen ausschliesslich über Konfiguration oder über zusätzliche, eigenentwickelte Pipe-Module.
