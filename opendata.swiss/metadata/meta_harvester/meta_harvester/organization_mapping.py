# Manual mapping between CKAN organization ids and I14Y slugs.
# This manually curated mapping becomes obsolete, when I14Y itself provides the mapping back to CKAN organization id.
# In the meantime, this mapping can be manually extended when necessary.
CKAN_TO_I14Y_ORG_MAPPING: dict[str, str] = {
    "abteilung-uebertragbare-krankheiten": "abteilung-uebertragbare-krankheiten",
    "ag": "ch-kt-ag",
    "agroscope": "ch-agroscope",
    "bernmobil": "bernmobil",
    "biel-bienne": "ch-gde-biel-bienne",
    "bkb": "ch-bkb",
    "bundesamt-fur-gesundheit-bag": "ch-bag",
    "bundesamt-fur-bevolkerungsschutz-babs": "ch-babs",
    "bundesamt-fur-energie-bfe": "ch-bfe",
    "bundesamt-fur-kommunikation-bakom": "ch-bakom",
    "bundesamt-fur-kultur-bak": "ch-bak",
    "bundesamt-fur-landestopografie-swisstopo": "ch-swisstopo",
    "bundesamt-fur-landwirtschaft-blw": "ch-blw",
    "bundesamt-fur-meteorologie-und-klimatologie-meteoschweiz": "ch-meteoschweiz",
    "bundesamt-fur-raumentwicklung-are": "ch-are",
    "bundesamt-fur-statistik-bfs": "ch1",
    "bundesamt-fur-strassen-astra": "ch-astra",
    "bundesamt-fur-umwelt-bafu": "ch-bafu",
    "bundesamt-fur-verkehr-bav": "ch-bav",
    "bundesamt-fur-zivildienst-zivi": "ch-zivi",
    "bundesamt-fur-zivilluftfahrt-bazl": "ch-bazl",
    "eidgenossische-finanzverwaltung-efv": "ch-efv",
    "eidgenoessische_zollverwaltung_ezv": "ch-bazg",
    "elcom": "ch-elcom",
    "fondation-modus": "ch-ge-fondationmodus",
    "gruppe-verteidigung": "ch-vtg",
    "identitas": "ch-identitas",
    "kanton-bern-2": "ch-kt-bern",
    "kanton-basel-landschaft": "ch-kt-bl",
    "kanton-basel-stadt": "ch-kt-bs",
    "kanton-st-gallen": "ch-kt-st-gallen",
    "lustat": "ch-lustat",
    "dienst-ueberwachung-post-und-fernmeldeverkehr-uepf": "ch-uepf",
    "direktion-fur-entwicklung-und-zusammenarbeit-deza": "ch-deza",
    "eth-bibliothek": "ch-eth-lib",
    "eth-zuerich": "ch-eth",
    "gemeinde_emmen": "ch-gde-emmen",
    "gemeinde_koeniz": "ch-gde-koeniz",
    "kanton-graubuenden": "ch-kt-gr",
    "kanton-schaffhausen": "ch-kt-sh",
    "kanton-schwyz": "ch-kt-sz",
    "kanton-thurgau": "ch-kt-tg",
    "kanton-uri": "ch-kt-uri",
    "kanton-zuerich": "ch-kt-zh",
    "swissmedic": "ch-swissmedic",
    "kanton_freiburg": "ch-kt-fr",
    "kanton_solothurn": "ch-kt-so",
    "lausanne": "ch-gde-lausanne",
    "morges": "ch-gde-morges",
    "public-sector-transformation": "ch-bfh-ipst",
    "sbfi": "ch-sbfi",
    "schweizerische-bundesbahnen-sbb": "ch-sbb",
    "schweizerische-nationalbibliothek-nb": "ch-nabibl",
    "schweizerisches-bundesarchiv-bar": "ch-bar",
    "swiss-emobility": "ch-swiss-emobility",
    "schweizerisches-nationalmuseum-snm": "ch-snm",
    "staatssekretariat-fuer-migration-sem": "ch-sem",
    "stadt-bern": "ch-gde-bern",
    "stadt-luzern": "ch-gde-luzern",
    "stadt-uster": "ch-gde-uster",
    "stadt-winterthur": "ch-gde-winterthur",
    "stadt-zug": "ch-gde-zug",
    "stadt-zurich": "ch-gde-zurich",
    "ti": "ch-kt-ti",
    "ville-geneve": "ch-gde-geneve",
    "canton-du-jura": "ch-kt-ju",
    "canton-geneve": "ch-kt-ge",
}


def resolve_i14y_publisher_slug(
    ckan_org_id: str | None,
) -> str | None:
    """
    Resolves CKAN organization ids to I14Y organization slugs.

    Resolution strategy:
    1) Manual mapping table (CKAN_TO_I14Y_ORG_MAPPING)
    """
    if ckan_org_id and ckan_org_id in CKAN_TO_I14Y_ORG_MAPPING:
        return CKAN_TO_I14Y_ORG_MAPPING[ckan_org_id]

    return None
