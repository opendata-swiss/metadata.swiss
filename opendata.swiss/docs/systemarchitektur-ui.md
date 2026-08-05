# UI

## Kurzbeschreibung

Das UI ist derjenige Anwendungsblock von ODSn, der die gesamte Benutzeroberfläche des Portals bereitstellt. Es ist als serverseitig gerenderte Webanwendung auf Basis von Nuxt (Vue 3) realisiert, wird als Container-Image betrieben und liefert alle Inhalte in den vier Sprachen Deutsch, Französisch, Italienisch und Englisch aus.

Das UI realisiert die Geschäftsfunktionen CMS, Handbook und Blog vollständig sowie die Geschäftsfunktionen Catalog und Showcases gemeinsam mit piveau. Die Arbeitsteilung ist dabei durchgängig gleich: piveau hält und verarbeitet die Metadaten, das UI erschliesst sie für die Nutzenden. Das UI betreibt keine eigene Metadatenhaltung und keine eigene Datenbank. Es bezieht seine Inhalte aus zwei Quellen — den Metadaten aus piveau (Komponenten `repo` und `search` des `hub`) und den redaktionellen Inhalten aus dem CMS-Content-Repository auf GitHub, die als Markdown-Dateien versioniert und beim Build in die Anwendung eingebunden werden. Ergänzend nutzt das UI listmonk für Abonnements und Benachrichtigungen, Hyvor für Kommentare und Bewertungen sowie Matomo für die Nutzungsstatistik.

Ausserhalb der Systemgrenze des UI liegen die Metadatenverarbeitung einschliesslich Harvesting, Validierung und Qualitätsbewertung (piveau), die Benutzerverwaltung und Authentisierung (eIAM, Keycloak) sowie die Persistenz der Metadaten (GraphDB, Elasticsearch).

## Abbildung

**Abb. X – Subsystem UI: Komponenten und Schnittstellen**

_(ArchiMate-View «UI», Export: `opendata.swiss/archimate/export/UI.png`)_

## Komponenten

| Komponente | Zweck | Dokumentation |
| --- | --- | --- |
| `opendata-swiss-ui` | Portalanwendung: Startseite und redaktionelle Seiten, Datensatzsuche mit Facetten, Datensatz- und Distributionsansichten, Showcases, Handbuch, Blog, Abonnementverwaltung sowie Einbindung der Kommentar- und Bewertungsfunktion. | [README](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/README.md) |
| `decap-cms-app` | Redaktionsoberfläche auf Basis von Decap CMS unter `/admin` zur Pflege von Seiten, Blogbeiträgen, Handbuchartikeln und Showcases. Änderungen werden als Pull Request in das CMS-Content-Repository geschrieben und dort über einen Redaktionsworkflow (Draft, In Review, Ready) freigegeben. | [CMS](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/cms.md), [Seiten](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/cms/pages.md), [Handbuch](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/cms/handbook.md) |
| `cms-assets-sync` | Build-Modul, das die im CMS gepflegten Medien aus dem Content-Repository in die statisch ausgelieferten Ressourcen der Anwendung übernimmt. | [Quellcode](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/app/modules/cms-assets-sync.ts) |
| `Showcases API` | Stellt die im CMS gepflegten Showcases maschinenlesbar als JSON-LD bereit, damit piveau sie harvesten kann, und nimmt öffentliche Showcase-Einreichungen entgegen (Validierung, Bildverarbeitung, Erzeugung eines Pull Requests). | [Showcases](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/cms/showcases.md) |
| `Subscribe API` | Nimmt Abonnements auf Datensätze, Kategorien und Organisationen entgegen und legt die Abonnentinnen und Abonnenten in listmonk an bzw. ergänzt bestehende Einträge. | [Abonnemente](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/subscriptions/index.md) |
| `Subscription API` | Verwaltet die Abopräferenzen (Sprache, Frequenz, abonnierte Objekte) und erzeugt die periodischen Benachrichtigungen über neu publizierte Datensätze. | [Abonnemente](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/docs/subscriptions/index.md) |
| `Webhooks API` | Nimmt Ereignisse des Kommentar- und Bewertungsdienstes entgegen: Neue Kommentare lösen eine Benachrichtigung der datenpublizierenden Stelle aus, Bewertungen werden an piveau zurückgeschrieben. | [Quellcode](https://github.com/opendata-swiss/metadata.swiss/blob/main/opendata.swiss/ui/server/api/webhooks/hyvor.post.ts) |

## Schnittstellen

| Gegenstelle | Richtung | Technik | Zweck |
| --- | --- | --- | --- |
| piveau `hub` / `search` | ausgehend | HTTPS, REST (JSON) | Datensatzsuche, Facetten und Detailansichten; Ermittlung neu publizierter Datensätze für die Benachrichtigungen; Ermittlung der Kontaktstelle eines Datensatzes |
| piveau `hub` / `repo` | ausgehend | HTTPS, RDF (Custom Resources) | Lesen und Zurückschreiben von Showcase-Ressourcen, insbesondere der aggregierten Bewertungen; authentisiert über ein Keycloak-Servicekonto |
| piveau (Harvesting) | eingehend | HTTPS, JSON-LD | piveau ruft die Showcases API ab — nach Plan sowie nach Änderungen an den Inhalten ausgelöst — und übernimmt die Showcases in den Katalog |
| GitHub – CMS-Content-Repository | aus- und eingehend | Git, GitHub API | Bezug der redaktionellen Inhalte beim Build; Ablage von Redaktions- und Einreichungsänderungen als Pull Request |
| GitHub App | ausgehend | GitHub API | Technische Identität des UI gegenüber GitHub: Commits und Pull Requests für Showcase-Einreichungen sowie Auslösen des Harvesting-Workflows |
| Keycloak / eIAM | ausgehend | OIDC | Anmeldung der Benutzenden; Bezug von Servicekonto-Tokens für schreibende Zugriffe auf piveau |
| listmonk | ausgehend | HTTPS, REST | Verwaltung der Abonnentinnen und Abonnenten sowie Versand der Benachrichtigungs- und Transaktions-E-Mails |
| Hyvor | aus- und eingehend | Widget (Browser), Webhook (HMAC-signiert) | Kommentieren und Bewerten von Datensätzen, Showcases, Handbuch und Blog; Rückmeldung der Ereignisse an die Webhooks API |
| Matomo | ausgehend (Browser) | HTTPS | Erhebung der Nutzungsstatistik des Portals |

## Datenfluss

Das UI ist in Bezug auf die Metadaten ausschliesslich lesend: DCAT-AP-CH-konforme Katalog-, Datensatz- und Distributionsdaten sowie die Qualitätsangaben werden aus piveau bezogen und dargestellt. In der Gegenrichtung bestehen zwei schreibende Flüsse: Showcases entstehen redaktionell oder durch öffentliche Einreichung im UI und werden von piveau über die Showcases API harvestet, und Bewertungen werden über die Webhooks API in die Showcase-Ressourcen von piveau zurückgeschrieben. Redaktionelle Inhalte (Seiten, Blog, Handbuch, Showcases) werden im Git-Repository des CMS versioniert und sind damit vollständig nachvollziehbar; Abonnementdaten liegen in listmonk, Kommentare und Bewertungen bei Hyvor. Das Datenmodell der Metadaten ist in Kapitel 1.3 beschrieben.
