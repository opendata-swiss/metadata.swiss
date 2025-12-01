import argparse
import json
import logging
import os
import time
from pathlib import Path
from typing import Union

import yaml
from dotenv import load_dotenv
from rdflib import BNode, Graph, Literal, Namespace, URIRef
from rdflib.namespace import DCAT, DCTERMS, FOAF, RDF, XSD
from requests.exceptions import HTTPError

from .api_clients import CkanClient, PiveauClient

CATALOGUES_PATH = os.getenv("CATALOGUES_PATH", "../piveau_catalogues")


logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

TEMPLATE_FILE = "src/meta_harvester/pipe-template.yaml"
PIPES_PATH = "../piveau_pipes"


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

def delete_catalogues(catalogue_names: list[str]) -> None:
    """
    Deletes catalogues from the piveau-hub-repo.
    """
    piveau_client = PiveauClient()
    logger.info(f"Deleting catalogues: {catalogue_names}")
    piveau_client.delete_catalogues(names=catalogue_names)



def create_single_catalogue(name: str, file_path: str | None = None) -> None:
    """
    Creates or updates a single catalogue from a file.
    If file_path is not provided, it defaults to the standard location.
    """
    piveau_client = PiveauClient()

    if not file_path:
        piveau_client.create_catalogues([name])
    else:
        metadata_file = Path(file_path)

        if not metadata_file.is_file():
            logger.error(f"Metadata file for '{name}' not found at '{metadata_file}'. Aborting.")
            return

        logger.info(f"Publishing catalogue '{name}' from file '{metadata_file}'")
        piveau_client.create_catalogue(name=name, metadata_file=str(metadata_file))

def generate_pipe(
    id: str,
    name: str,
    org_name: str,
    catalogue: str,
    title: str,
    http_client: str,
    template_file: str = TEMPLATE_FILE,
    output_path: str = PIPES_PATH,
) -> None:
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
    data["body"]["segments"][0]["body"]["config"]["org_name"] = org_name

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
) -> None:

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


def generate_pipe_and_catalogue_files(pipes: bool = True, catalogues: bool = True)-> None:
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

        org_id = ckan_client.get_org_id_for_harvester(id)
        if org_id:
            full_org_details = ckan_client.get_organization_details(organization_id=org_id)
            if full_org_details:
                org_url = full_org_details.get("url", "https://example.com")
            else:
                logging.warning(
                    f"No org url for '{catalogue_name}' provided."
                )
                org_url = "https://example.com"
        else:
            org_url = "https://example.com"


        organization = to_dict(details.get("organization", {}))
        org_titles = to_dict(organization.get("title", "{}"))

        if catalogues:
            generate_catalogue_metadata(
                catalogue_name=catalogue_name,
                org_titles=org_titles,
                org_descriptions=to_dict(organization.get("description", "{}")),
                created=details["metadata_created"],
                modified=details["metadata_modified"],
                homepage=org_url,
            )

        if pipes:
            generate_pipe(
                id=id,
                name=details["name"],
                org_name=org_titles.get("en", "unknown_org"),
                catalogue=catalogue_name,
                title=details["title"],
                http_client=url,
            )


def run_pipes(pipe_names: list | None = None, create_catalogue: bool = False)-> None:

    """
    Triggers piveau pipes to run, respecting a maximum number of concurrent runs.
    Optionally, it ensures the corresponding catalogue is created/updated first.
    If no pipe names are provided, triggers all pipes found in the PIPES_PATH directory.
    """
    piveau_client = PiveauClient()

    if not pipe_names:
        logging.info(f"No specific pipes provided. Discovering pipes in '{PIPES_PATH}'...")
        pipe_dir = Path(PIPES_PATH)
        if not pipe_dir.is_dir():
            logging.error(f"Pipes directory not found: {PIPES_PATH}")
            return
        # Get the name from the filename without the .yaml extension
        pipe_names = sorted([p.stem for p in pipe_dir.glob("*.yaml")])

    if not pipe_names:
        logging.warning("No pipes found to trigger.")
        return

    logging.info(f"Queueing {len(pipe_names)} pipe(s) for execution...")
    for name in pipe_names:

        while True:
            active_runs = piveau_client.run_client.list_runs(run_filter=["active"])
            active_runs_names = [i["pipeHeader"]["name"] for i in active_runs if i["status"] == "active"]

            if name in active_runs_names:
                logger.info(f"Pipe '{name}' is already running. Skipping trigger.")
                break

            active_count = len(active_runs)
            logger.info(f"Currently {active_count} active run(s).")

            if active_count < piveau_client.max_concurrent_runs:
                logger.info("Slot available. Proceeding to launch pipe.")
                break

            logger.info(
                f"Reached maximum concurrent runs ({piveau_client.max_concurrent_runs}). Waiting for a slot to open..."
            )
            time.sleep(30)


        if create_catalogue:
            # Derive catalogue name from pipe name and create it
            catalogue_name = name.replace("-geocat-harvester", "")
            logging.info(
                f"Creating/updating '{catalogue_name} before running pipe '{name}'."
            )
            piveau_client.create_catalogues([catalogue_name], recreate=create_catalogue)

        piveau_client.trigger_pipe(pipe_name=name)
        time.sleep(5)

def create_catalogues_wrapper(catalogue_names: list[str])-> None:
    """
    Creates or updates catalogues from .ttl files.
    If no names are provided, all catalogues in the CATALOGUES_PATH directory are created/updated.
    """
    piveau_client = PiveauClient()
    piveau_client.create_catalogues(catalogue_names)


def main()-> None:
    # Load .env file for environment variables
    load_dotenv()

    parser = argparse.ArgumentParser(description="Meta-Harvester CLI tool.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Sub-command for generating pipes and catalogues
    parser_generate = subparsers.add_parser(
        "generate", help="Generate all pipes and catalogue files from CKAN."
    )
    parser_generate.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=True)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-pipes", help="Generate all pipe definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=False)

    # Sub-command for running pipes
    parser_run = subparsers.add_parser("run-pipes", help="Trigger pipes to run.")

    parser_run.add_argument(
        "pipes",
        nargs="*",
        help="Optional: A list of specific pipe names to run. If omitted, all pipes will be run.",
    )
    parser_run.add_argument(
        "--create-catalogue",
        action="store_true",
        dest="create_catalogue",
        help="Create the catalogue before running the pipe.",
    )
    parser_run.set_defaults(func=run_pipes)

    parser_delete = subparsers.add_parser(
        "delete-catalogues", help="Delete catalogues by name. If no names are provided, all catalogues will be deleted."
    )
    parser_delete.add_argument(
        "names", nargs="*", help="Optional: A list of catalogue names to delete."
    )
    parser_delete.set_defaults(func=delete_catalogues)

    parser_create = subparsers.add_parser(
        "create-catalogues", help="Create catalogues from .ttl files. If no names are provided, all catalogues will be created."
    )
    parser_create.add_argument(
        "names",
        nargs="*",
        help="Optional: A list of catalogue names to create. The script will look for a corresponding .ttl file inside 'piveau_catalogues/'.",
    )
    parser_create.set_defaults(func=create_catalogues_wrapper)


    parser_create_from_file = subparsers.add_parser(
        "create-single-catalogue",
        help="" \
        "Create a single catalogue from a specific .ttl file."
    )
    parser_create_from_file.add_argument(
        "name",
        help="The target name for the catalogue."
    )
    parser_create_from_file.add_argument(
        "--file",
        dest="file_path",
        help="Optional: Path to the .ttl file. If omitted, defaults to the standard path."
    )
    parser_create_from_file.set_defaults(func=create_single_catalogue)


    args = parser.parse_args()

    if args.command == "run-pipes":
        args.func(pipe_names=args.pipes, create_catalogue=args.create_catalogue)
    elif args.command == "delete-catalogues":
        args.func(catalogue_names=args.names if args.names else [])
    elif args.command == "create-catalogues":
        args.func(catalogue_names=args.names if args.names else [])
    elif args.command == "create-single-catalogue":
        args.func(name=args.name, file_path=args.file_path)
    elif args.command in ["generate", "generate-all-pipes"]:
        args.func(pipes=args.pipes, catalogues=args.catalogues)
    else:
        args.func()

if __name__ == "__main__":
    main()
