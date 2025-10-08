import logging
from pathlib import Path
from typing import Union

import yaml
from rdflib import BNode, Graph, Literal, Namespace, URIRef
from rdflib.namespace import DCAT, DCTERMS, FOAF, RDF, XSD
from requests.exceptions import HTTPError

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

from .api_clients import CkanClient, PiveauClient

TEMPLATE_FILE = "src/meta_harvester/pipe-template.yaml"
PIPES_PATH = "../piveau_pipes"
CATALOGUES_PATH = "../piveau_catalogues"
import json


def to_dict(value: Union[str, dict]) -> dict:
    """
    Safely converts a JSON string to a dictionary.
    If the value is already a dictionary, it returns it directly.
    """
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            logger.error(f"Could not decode JSON string: {value}")
            return {}
    return {}


def generate_pipe(
    id: str,
    name: str,
    title: str,
    http_client: str,
    template_file: str = TEMPLATE_FILE,
    output_path: str = PIPES_PATH,
):
    """
    Reads a template pipe file, update selected fields, and saves it as an execution-ready pipe.
    """
    with open(template_file, "r") as file:
        data = yaml.safe_load(file)

    data["header"]["id"] = id
    data["header"]["name"] = name
    data["header"]["title"] = title

    data["body"]["segments"][0]["body"]["config"]["address"] = http_client
    data["body"]["segments"][0]["body"]["config"]["catalogue"] = name

    output_file = Path(output_path) / f"{name}.yaml"

    with open(output_file, "w") as file:
        yaml.dump(data, file, sort_keys=False, indent=2)

    logger.info(f"Successfully generated '{output_file}'")


def generate_catalogue_metadata(
    catalogue_name: str,
    org_titles: dict,
    org_descriptions: dict,
    created: str,
    modified: str,
    homepage: str = "https://example.com",
) -> str:

    EU_LANG = Namespace("http://publications.europa.eu/resource/authority/language/")
    EU_COUNTRY = Namespace("http://publications.europa.eu/resource/authority/country/")
    EX = Namespace("https://example.eu/id/catalogue/")

    g = Graph()

    g.bind("dcat", DCAT)
    g.bind("dcterms", DCTERMS)
    g.bind("foaf", FOAF)
    g.bind("xsd", XSD)

    catalogue_uri = EX[catalogue_name]

    # Publisher
    publisher_bnode = BNode()
    g.add((catalogue_uri, DCTERMS.publisher, publisher_bnode))
    g.add((publisher_bnode, RDF.type, FOAF.Agent))
    g.add((publisher_bnode, FOAF.homepage, URIRef(homepage)))

    # Catalogue
    g.add((catalogue_uri, RDF.type, DCAT.Catalog))
    g.add((catalogue_uri, DCTERMS.spatial, EU_COUNTRY.CHE))
    g.add((catalogue_uri, DCTERMS.type, Literal("ckan", datatype=XSD.string)))

    for lang, title in org_titles.items():
        if title:
            g.add((catalogue_uri, DCTERMS.title, Literal(title, lang=lang)))
            g.add((publisher_bnode, FOAF.name, Literal(title, lang=lang)))

    for lang, desc in org_descriptions.items():
        if desc:
            g.add((catalogue_uri, DCTERMS.description, Literal(desc, lang=lang)))

    g.add((catalogue_uri, DCTERMS.created, Literal(created, datatype=XSD.dateTime)))
    g.add((catalogue_uri, DCTERMS.modified, Literal(modified, datatype=XSD.dateTime)))

    output_file = Path(CATALOGUES_PATH) / f"{catalogue_name}.ttl"
    g.serialize(destination=output_file, format="turtle")
    logger.info(f"Successfully generated RDF triples and saved to '{output_file}'")

    return output_file


def main():
    ckan_client = CkanClient()
    ids = ckan_client.get_geoharvesters_ids()

    print(ids[-1])
    print(f"Collected {len(ids)} geoharvester(s)")

    piveau_client = PiveauClient()

    for id in ids:
        try:
            details = ckan_client.get_harvester_details_by_id(id)
        except HTTPError as e:
            if e.response.status_code == 403:
                logger.warning(f"Access forbidden for harvester ID {id}. Omitting this catalogue.")
                continue

        url = details["url"].split("?")[0]

        generate_pipe(
            id=id, name=details["name"], title=details["title"], http_client=url
        )

        catalogue_name = details["name"].replace("-geocat-harvester", "")
        metadata_file = generate_catalogue_metadata(
            catalogue_name=catalogue_name,
            org_titles=to_dict(details["organization"].get("title", {})),
            org_descriptions=to_dict(details["organization"].get("description", {})),
            created=details["metadata_created"],
            modified=details["metadata_modified"],
            homepage="https://example.com",
        )


        piveau_client.create_catalogue(
            name=catalogue_name, metadata_file=metadata_file
        )

        #piveau_client.delete_catalogue(
        #    name=catalogue_name
        #)


# --- Example Usage ---
if __name__ == "__main__":
    main()
