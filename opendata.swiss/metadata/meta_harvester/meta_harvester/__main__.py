import argparse
import json
import logging
import os
import time
from datetime import datetime
from pathlib import Path
from typing import Union

import yaml
from rdflib import BNode, Graph, Literal, Namespace, URIRef
from rdflib.namespace import DCAT, DCTERMS, FOAF, RDF, XSD
from requests.exceptions import HTTPError

from .api_clients import CkanClient, PiveauClient, PiveauRunClient

CATALOGUES_PATH = os.getenv("CATALOGUES_PATH", "../piveau_catalogues")


logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

TEMPLATE_FILE = "meta_harvester/pipe-template.yaml"
PIPES_PATH = "../piveau_pipes"


def to_dict(value: Union[str, dict]) -> dict:
    """
    Converts a JSON string to a dictionary.

    Args:
        value (Union[str, dict]):       JSON string to convert.

    Returns:
        dict:                           The converted dictionary, or an empty dictionary if conversion fails.
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

    Args:
        catalogue_names (list[str]):    A list of catalogue names to delete.
    """
    piveau_client = PiveauClient()
    logger.info(f"Deleting catalogues: {catalogue_names}")
    piveau_client.delete_catalogues(names=catalogue_names)



def create_single_catalogue(name: str, file_path: str | None = None) -> None:
    """
    Creates or updates a single catalogue from a file.
    If file_path is not provided, it defaults to the default location.

    Args:
        name      (str):                        The target name for the catalogue.
        file_path (str | None, optional):       Path to the .ttl file.
                                                If omitted, defaults to the default path.
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
    cluster: bool = True
) -> str:
    """
    Reads a template pipe file, updates selected fields, and saves it as an execution-ready pipe.

    Args:
        id            (str): The ID of the pipe.
        name          (str): The name of the pipe, used as the output filename.
        org_name      (str): The name of the organization.
        catalogue     (str): The name of the associated catalogue.
        title         (str): The human-readable title of the pipe.
        http_client   (str): The URL of the CKAN harvester endpoint.
        template_file (str, optional): Path to the pipe template file.
        output_path   (str, optional): Directory to save the generated pipe file.
    """
    with open(template_file, "r") as file:
        data = yaml.safe_load(file)

    data["header"]["id"] = id
    data["header"]["name"] = name
    data["header"]["title"] = title

    data["body"]["segments"][0]["body"]["config"]["address"] = http_client
    data["body"]["segments"][0]["body"]["config"]["catalogue"] = catalogue
    data["body"]["segments"][0]["body"]["config"]["org_name"] = org_name

    output_file = Path(output_path) / f"{name}.json"

    with open(output_file, "w") as file:
        json.dump(data, file, indent=2)

    logger.info(f"Successfully generated '{output_file}'")

    return str(output_file)



def generate_catalogue_metadata(
    catalogue_name: str,
    org_titles: dict,
    org_descriptions: dict,
    created: str,
    modified: str,
    homepage: str = "https://example.com",
) -> None:
    """
    Generates an RDF metadata file for a catalogue in Turtle format.

    Args:
        catalogue_name (str):       The name of the catalogue.
        org_titles (dict):          A dictionary of titles for the organization, with language codes as keys.
        org_descriptions (dict):    A dictionary of descriptions, with language codes as keys.
        created (str):              The creation timestamp of the metadata (ISO 8601 format).
        modified (str):             The modification timestamp of the metadata (ISO 8601 format).
        homepage(str, optional):    The URL to the publisher's homepage.

    Returns:
        None
    """
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


def generate_pipe_and_catalogue_files(pipes: bool = True, catalogues: bool = True, cluster: bool = True) -> None:
    """
    Fetches all geoharvesters from CKAN and generates corresponding
    pipe and catalogue metadata files.

    Args:
        pipes      (bool, optional):    If True, generates pipe definition files. Defaults to True.
        catalogues (bool, optional):    If True, generates catalogue metadata files. Defaults to True.
        cluster    (bool, optional):    True for using in cluster mode (API-based pipes updates), False for local mode (file-based pipes updates). Defaults to True.
    """
    ckan_client = CkanClient()

    try:
        ids = ckan_client.get_geoharvesters_ids()
        logging.info(f"Collected {len(ids)} geoharvester(s) from CKAN.")
    except HTTPError as e:
        logging.error(f"Failed to fetch harvester IDs from CKAN: {e}")
        return

    if pipes and cluster:
        piveau_run_client = PiveauRunClient()

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
            output_file =generate_pipe(
                id=id,
                name=details["name"],
                org_name=org_titles.get("en", "unknown_org"),
                catalogue=catalogue_name,
                title=details["title"],
                http_client=url,
                cluster=cluster
            )
            if cluster:
                piveau_run_client.upload_pipe(pipe_file=output_file)

def run_pipes(pipe_names: list | None = None, create_catalogue: bool = False, include_static: bool = False) -> None:
    """
    Triggers piveau pipes to run, respecting a maximum number of concurrent runs.
    Optionally, it ensures the corresponding catalogue is created/updated first.
    If no pipe names are provided, triggers all pipes found in the PIPES_PATH directory.

    Args:
        pipe_names (list | None, optional):       A list of specific pipe names to run.
                                                  If omitted, all pipes will be run. Defaults to None.
        create_catalogue (bool, optional):        If True, creates the catalogue before running the pipe.
                                                  Defaults to False.
        include_static (bool, optional):          If True, includes static pipes to be run. Defaults to False.
    """
    piveau_client = PiveauClient()
    piveau_run_client = PiveauRunClient()

    if not pipe_names:
        pipe_names = piveau_run_client.list_pipes(include_static)

    if not pipe_names:
        logging.warning("No pipes found to trigger.")
        return

    logging.info(f"Queueing {len(pipe_names)} pipe(s) for execution...")
    time.sleep(60) # Wait for the service to be up to date
    for name in pipe_names:

        while True:
            runs_metadata = piveau_client.run_client.list_runs()
            runs = {i["pipeHeader"]["name"]: {"status": i["status"], "startTime": i["pipeHeader"]["startTime"]} for i in runs_metadata}

            if name in runs:
                if runs[name]["status"] == "active":
                    logger.info(f"Pipe '{name}' is already running. Skipping trigger.")
                    break
                if runs[name]["status"] == "finished":
                    pipe_started_at = datetime.strptime(runs[name]["startTime"], "%Y-%m-%dT%H:%M:%S.%f%z")
                    elapsed_time = (datetime.now(pipe_started_at.tzinfo) - pipe_started_at).total_seconds()/3600
                    if elapsed_time < piveau_client.min_hours_between_runs:
                        logger.info(f"Pipe '{name}' has already finished. It started at {runs[name]['startTime']}. Skipping trigger.")
                    break

            active_count = sum([1 for run in runs.values() if run["status"] == "active"])
            logger.info(f"Currently {active_count} active run(s).")

            if active_count < piveau_client.max_concurrent_runs:
                logger.info("Slot available. Proceeding to launch pipe.")
                break

            logger.info(
                f"Reached maximum concurrent runs ({piveau_client.max_concurrent_runs}). Waiting for a slot to open..."
            )
            time.sleep(60)


        if create_catalogue:
            catalogue_name = name.replace("-geocat-harvester", "")
            logging.info(
                f"Creating/updating '{catalogue_name} before running pipe '{name}'."
            )
            piveau_client.create_catalogues([catalogue_name], recreate=create_catalogue)

        piveau_client.trigger_pipe(pipe_name=name)
        time.sleep(5)


def create_catalogues_wrapper(catalogue_names: list[str]) -> None:
    """
    Creates or updates catalogues from .ttl files.
    If no names are provided, all catalogues in the CATALOGUES_PATH directory are created/updated.

    Args:
        catalogue_names (list[str]):       A list of catalogue names to create.
    """
    piveau_client = PiveauClient()
    piveau_client.create_catalogues(catalogue_names)


def main()-> None:

    parser = argparse.ArgumentParser(description="Meta-Harvester CLI tool.")
    subparsers = parser.add_subparsers(dest="command", required=True)


    parser_generate = subparsers.add_parser(
        "generate", help="Generate all pipes and catalogue files from CKAN."
    )
    parser_generate.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=True, cluster=False)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-pipes", help="Generate all pipe definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=False, cluster=False)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-catalogues", help="Generate all catalogue definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_pipe_and_catalogue_files, pipes=False, catalogues=True, cluster=False)


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
    parser_run.add_argument(
        "--include-static",
        action="store_true",
        dest="include_static",
        help="Include static pipes when running.",
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
    elif args.command in ["generate", "generate-all-pipes", "generate-all-catalogues"]:
        args.func(pipes=args.pipes, catalogues=args.catalogues)
    else:
        args.func()

if __name__ == "__main__":
    main()
