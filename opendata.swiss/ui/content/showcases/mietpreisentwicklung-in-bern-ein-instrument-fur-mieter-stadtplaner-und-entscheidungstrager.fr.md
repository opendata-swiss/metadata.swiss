---
active: true
title: Évolution des loyers à Berne – Un outil pour les locataires, urbanistes
  et décideurs
image: https://repository-images.githubusercontent.com/788945570/a1998415-fef9-4518-8a5d-d2937f17edec
url: https://giodi.github.io/dashboard-wohnungsmietpreise-stadt-bern/
categories:
  - http://publications.europa.eu/resource/authority/data-theme/SOCI
  - http://publications.europa.eu/resource/authority/data-theme/REGI
type: https://opendata.swiss/vocabulary/showcase-type/application
datasets:
  - id: 523-staatskanzlei-kanton-zuerich
    label: "Kanton Bern: Gebäude, Wohnungen und Wohnverhältnisse"
---
Ce tableau de bord **interactif** permet de visualiser l’évolution des **loyers à Berne** entre 2013 et 2024, par **quartier**, **année** et **taille de logement**. Il facilite la compréhension des dynamiques du marché locatif et offre une vue d’ensemble claire, utile à la fois pour les **citoyens**, les **acteurs publics** et les **professionnels du logement**.

Le tableau de bord a été développé dans le cadre des modules *Data Visualization* et *Dashboard Design* à la **Fachhochschule Graubünden** par **Lukas Streit**, **Martina Stüssi** et **Gionathan Diani**, avec l’objectif de renforcer la **transparence immobilière**.

L’application repose sur des technologies modernes comme **11ty** (Static Site Generator) et **Apache ECharts**, garantissant une expérience fluide, rapide et responsive.

## Données utilisées

Les données proviennent du service statistique de la **Ville de Berne**, avec une couverture annuelle des prix moyens des loyers par quartier et type de logement.

Le jeu de données a été retravaillé en raison d’un format initial inadapté au traitement automatique. À partir de 2013, une colonne « Insgesamt » a été ajoutée, renommée « Total » en 2015. Elle contient la moyenne pondérée par le nombre de logements par quartier, les données sur le nombre de logements étant fournies dans un jeu de données séparé. Le coût par pièce a été calculé séparément. En raison de l’impossibilité de reconstituer les valeurs manquantes pour « Total », les années 2010 à 2012 ont été exclues du tableau de bord.

## Cas d'usages concrets

***Locataires & personnes en recherche de logement :***

* Comparez les loyers entre quartiers
* Suivez les tendances et évitez de payer trop cher
* Argumentez en toute transparence lors de négociations

***Urbanisme & politiques publiques :***

* Appuyez des décisions d’aménagement avec des données concrètes
* Surveillez les évolutions dans une logique d’équité territoriale

***Recherche & journalisme :***

* Étudiez les dynamiques du logement à l’échelle locale
* Intégrez des visualisations directement dans vos analyses

## Accès et conditions d'utilisation

L’outil est gratuit, accessible à tous, sans inscription, et s’appuie uniquement sur des données ouvertes. 🔗 Accéder à l’application : <https://giodi.github.io/dashboard-wohnungsmietpreise-stadt-bern/> 💻 Code source : <https://github.com.mcas.ms/giodi/dashboard-wohnungsmietpreise-stadt-bern>

Explorez le tableau de bord dès maintenant et voyez où se trouvent les loyers les plus adaptés à vos besoins
