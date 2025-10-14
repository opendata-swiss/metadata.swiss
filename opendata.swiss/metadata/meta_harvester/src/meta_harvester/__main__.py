import argparse
import json
import logging
from pathlib import Path
from typing import Union

import yaml
from dotenv import load_dotenv
from rdflib import BNode, Graph, Literal, Namespace, URIRef
from rdflib.namespace import DCAT, DCTERMS, FOAF, RDF, XSD
from requests.exceptions import HTTPError

from .api_clients import CkanClient, PiveauClient

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

TEMPLATE_FILE = "src/meta_harvester/pipe-template.yaml"
PIPES_PATH = "../piveau_pipes"
CATALOGUES_PATH = "../piveau_catalogues"

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

def delete_catalogues(catalogue_names: list[str]):
    """
    Deletes one or more catalogues from the piveau-hub-repo.
    """
    piveau_client = PiveauClient()
    for name in catalogue_names:
        logger.info(f"Attempting to delete catalogue: {name}")
        piveau_client.delete_catalogue(name=name)


def create_catalogues(catalogue_names: list[str]):
    """
    Creates or recreates one or more catalogues in the piveau-hub-repo.
    For each name, it derives the metadata file path from the CATALOGUES_PATH.
    If a catalogue exists, it is deleted first.
    """
    piveau_client = PiveauClient()
    for name in catalogue_names:
        metadata_file = Path(CATALOGUES_PATH) / f"{name}.ttl"
        if not metadata_file.is_file():
            logger.error(
                f"Metadata file for '{name}' not found at '{metadata_file}'. Skipping."
            )
            continue

        logger.info(f"Recreating catalogue '{name}' from file '{metadata_file}'")

        piveau_client.delete_catalogue(name=name)
        piveau_client.create_catalogue(name=name, metadata_file=str(metadata_file))


def generate_pipe(
    id: str,
    name: str,
    catalogue: str,
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
    data["body"]["segments"][0]["body"]["config"]["catalogue"] = catalogue

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

def create_all_catalogues():
    """
    Creates/recreates all catalogues from the .ttl files found in the local directory.
    """
    catalogue_dir = Path(CATALOGUES_PATH)
    if not catalogue_dir.is_dir():
        logger.error(f"Catalogues directory not found: {CATALOGUES_PATH}")
        return

    catalogue_files = list(catalogue_dir.glob("*.ttl"))
    if not catalogue_files:
        logger.warning(f"No catalogue files (.ttl) found in '{CATALOGUES_PATH}'.")
        return

    logger.info(f"Found {len(catalogue_files)} catalogues to create/recreate.")
    catalogue_names = [f.stem for f in catalogue_files]
    create_catalogues(catalogue_names)

def generate_pipes_and_catalogues():
    """
    Fetches all geoharvesters from CKAN and generates corresponding
    pipe and catalogue metadata files.
    """
    ckan_client = CkanClient()

    try:
        ids = ckan_client.get_geoharvesters_ids()
        logging.info(f"Collected {len(ids)} geoharvester(s) from CKAN.")
    except HTTPError as e:
        logging.error(f"Failed to fetch harvester IDs from CKAN: {e}")
        return

    for id in ids:
        try:
            details = ckan_client.get_harvester_details_by_id(id)
        except HTTPError as e:
            if e.response.status_code == 403:
                logging.warning(
                    f"Access forbidden for harvester ID {id}. Omitting this catalogue."
                )
            else:
                logging.error(
                    f"HTTP error for harvester ID {id}: {e}. Skipping."
                )
            continue

        url = details["url"].split("?")[0]
        catalogue_name = details["name"].replace("-geocat-harvester", "")

        generate_pipe(
            id=id,
            name=details["name"],
            catalogue=catalogue_name,
            title=details["title"],
            http_client=url,
        )

        organization = to_dict(details.get("organization", {}))
        generate_catalogue_metadata(
            catalogue_name=catalogue_name,
            org_titles=to_dict(organization.get("title", "{}")),
            org_descriptions=to_dict(organization.get("description", "{}")),
            created=details["metadata_created"],
            modified=details["metadata_modified"],
            homepage="https://example.com",
        )

        create_catalogues([catalogue_name])


def run_pipes(pipe_names: list = None):
    """
    Triggers piveau pipes to run. If no names are provided,
    triggers all pipes found in the PIPES_PATH directory.


    """
    piveau_client = PiveauClient()

    if not pipe_names:
        logging.info(f"No specific pipes provided. Discovering pipes in '{PIPES_PATH}'...")
        pipe_dir = Path(PIPES_PATH)
        if not pipe_dir.is_dir():
            logging.error(f"Pipes directory not found: {PIPES_PATH}")
            return
        # Get the name from the filename without the .yaml extension
        pipe_names = [p.stem for p in pipe_dir.glob("*.yaml")]

    if not pipe_names:
        logging.warning("No pipes found to trigger.")
        return

    logging.info(f"Triggering {len(pipe_names)} pipe(s)...")
    for name in pipe_names:
        piveau_client.trigger_pipe(pipe_name=name)

def generate_all_pipes():
    """
    Fetches all geoharvesters from CKAN and generates corresponding pipe files.
    """
    ckan_client = CkanClient()
    try:
        ids = ckan_client.get_geoharvesters_ids()
        logger.info(f"Collected {len(ids)} geoharvester(s) from CKAN.")
    except HTTPError as e:
        logger.error(f"Failed to fetch harvester IDs from CKAN: {e}")
        return

    logger.info("Generating all pipe definition files...")
    for id in ids:
        try:
            details = ckan_client.get_harvester_details_by_id(id)
        except HTTPError as e:
            if e.response.status_code == 403:
                logger.warning(
                    f"Access forbidden for harvester ID {id}. Omitting this pipe."
                )
            else:
                logger.error(f"HTTP error for harvester ID {id}: {e}. Skipping.")
            continue

        url = details["url"].split("?")[0]
        catalogue_name = details["name"].replace("-geocat-harvester", "")

        generate_pipe(
            id=id,
            name=details["name"],
            catalogue=catalogue_name,
            title=details["title"],
            http_client=url,
        )
    logger.info("Finished generating pipe files.")

def main():
    # Load .env file for environment variables
    load_dotenv()

    parser = argparse.ArgumentParser(description="Meta-Harvester CLI tool.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Sub-command for generating pipes and catalogues
    parser_generate = subparsers.add_parser(
        "generate", help="Generate all pipes and catalogues from CKAN."
    )
    parser_generate.set_defaults(func=generate_pipes_and_catalogues)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-pipes", help="Generate all pipe definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_all_pipes)

    # Sub-command for running pipes
    parser_run = subparsers.add_parser("run-pipes", help="Trigger pipes to run.")

    # Sub-command for running pipes
    parser_run = subparsers.add_parser("run-pipes", help="Trigger pipes to run.")
    parser_run.add_argument(
        "pipes",
        nargs="*",
        help="Optional: A list of specific pipe names to run. If omitted, all pipes will be run.",
    )
    parser_run.set_defaults(func=run_pipes)

    parser_delete = subparsers.add_parser(
        "delete-catalogues", help="Delete catalogues by name."
    )
    parser_delete.add_argument(
        "names", nargs="+", help="A list of catalogue names to delete."
    )
    parser_delete.set_defaults(func=delete_catalogues)


    parser_create = subparsers.add_parser(
        "create-catalogues", help="Create or recreate catalogues from files."
    )
    parser_create.add_argument(
        "names",
        nargs="+",
        help="A list of catalogue names to create. The script will look for a corresponding .ttl file.",
    )
    parser_create.add_argument(
        "file", help="The path to the .ttl metadata file for the catalogue."
    )
    parser_create.set_defaults(func=create_catalogues)

    parser_create_all = subparsers.add_parser(
        "create-all-catalogues",
        help="Create or recreate all catalogues from local .ttl files.",
    )
    parser_create_all.set_defaults(func=create_all_catalogues)

    args = parser.parse_args()

    if args.command == "run-pipes":
        args.func(pipe_names=args.pipes)
    elif args.command in ["delete-catalogues", "create-catalogues"]:
        args.func(catalogue_names=args.names)
    else:
        args.func()


if __name__ == "__main__":
    main()
