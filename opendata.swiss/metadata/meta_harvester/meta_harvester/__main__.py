import argparse
import json
import logging
import os
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Union

import requests
import yaml
from rdflib import BNode, Graph, Literal, Namespace, URIRef
from rdflib.namespace import DCAT, DCTERMS, FOAF, RDF, XSD, ORG, SKOS
from requests.exceptions import HTTPError

from .api_clients import CkanClient, I14YClient

CATALOGUES_PATH = os.getenv("CATALOGUES_PATH", "../piveau_catalogues")
PIPES_PATH = "../piveau_pipes"
TRIGGERS_PATH = "../piveau_triggers"
ORGANIZATIONS_PATH = "../piveau_organizations"
LEGAL_FORM_VOCAB_PATH = (
    Path(__file__).resolve().parents[2] / "piveau_vocabularies" / "i14y-legalForm.nt"
)
_LEGAL_FORM_LOOKUP = None

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

template_files = {
    "geocat_harvester": "meta_harvester/pipe-template.yaml",
    "dcat_ch_rdf" : "meta_harvester/pipe-template-rdf.yaml",
    "dcat_ch_i14y_rdf" : "meta_harvester/pipe-template-rdf.yaml"
}


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


def load_legal_form_lookup() -> dict:
    """
    Loads legal-form labels from the vocabulary file once and caches them.

    Returns:
        dict: Mapping by legal-form code, e.g. {"0220": {"id": ..., "label": ..., "resource": ...}}
    """
    global _LEGAL_FORM_LOOKUP

    if _LEGAL_FORM_LOOKUP is not None:
        return _LEGAL_FORM_LOOKUP

    schema = Namespace("http://schema.org/")
    lookup = {}
    graph = Graph()

    try:
        graph.parse(str(LEGAL_FORM_VOCAB_PATH), format="nt")
    except Exception as e:
        logger.error(f"Failed to load legal form vocabulary from '{LEGAL_FORM_VOCAB_PATH}': {e}")
        _LEGAL_FORM_LOOKUP = {}
        return _LEGAL_FORM_LOOKUP

    for subject, _, identifier in graph.triples((None, schema.identifier, None)):
        code = str(identifier)
        labels = {}

        for _, _, pref_label in graph.triples((subject, SKOS.prefLabel, None)):
            if isinstance(pref_label, Literal) and pref_label.language:
                labels[pref_label.language] = str(pref_label)

        # Fall back to schema:name if no SKOS labels are present.
        if not labels:
            for _, _, name in graph.triples((subject, schema.name, None)):
                if isinstance(name, Literal) and name.language:
                    labels[name.language] = str(name)

        lookup[code] = {
            "id": code,
            "label": labels,
            "resource": str(subject),
        }

    _LEGAL_FORM_LOOKUP = lookup
    return _LEGAL_FORM_LOOKUP




def generate_pipe(
    id: str,
    name: str,
    org_id: str,
    org_name: str,
    catalogue: str,
    title: str,
    http_client: str,
    template_file: str,
    output_path: str = PIPES_PATH
) -> str:
    """
    Reads a template pipe file, updates selected fields, and saves it as an execution-ready pipe.

    Args:
        id            (str): The ID of the pipe.
        name          (str): The name of the pipe, used as the output filename.
        org_id        (str): The ID of the organization.
        org_name      (str): The name of the organization.
        catalogue     (str): The name of the associated catalogue.
        title         (str): The human-readable title of the pipe.
        http_client   (str): The URL of the CKAN harvester endpoint.
        template_file (str): Path to the pipe template file.
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

    for segment in data["body"]["segments"]:
        if segment["header"]["name"] == "piveau-consus-filter":
            segment["body"]["config"]["org_id"] = org_id
            segment["body"]["config"]["catalogue"] = catalogue


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


def generate_bulk_triggers(pipe_names: list) -> None:
    # pair each pipe name with a date with 5 minutes difference, starting from now, to create staggered triggers    
    start_time = datetime.now()
    staggered_triggers = {}
    for i, pipe_name in enumerate(pipe_names):
        trigger_time = start_time + timedelta(minutes=5 * (i+1))
        trigger_time = trigger_time.astimezone(timezone.utc)
        staggered_triggers[pipe_name] = trigger_time.strftime("%Y-%m-%dT%H:%M:%S.000Z")

    with open(f"{TRIGGERS_PATH}/bulk.json", "w") as trigger_file:
        json.dump({
            trigger: [
                {
                    "interval": {
                        "value": 1,
                        "unit": "DAY"
                    },
                    "id": f"{trigger}-trigger",
                    "status": "enabled",
                    "next": staggered_triggers[trigger]
                }
            ]
            for trigger in staggered_triggers
        }, trigger_file, indent=2)


def generate_organization_metadata(
        slug: str,
        identifier: str,
        subAgentOf_slug: str,
        classification_code: str,
        names: dict,
        prefLabels: dict,
        descriptions: dict,
        homepage: str,
) -> None:
    """
    Generates an RDF metadata file for an organization in Turtle format.

    Args:
        slug (str):                The slug of the organization.
        identifier (str):          The identifier of the organization.
        subAgentOf_slug (str):     The slug of the parent organization, if any.
        classification_code (str): The classification code of the organization.
        names (dict):              A dictionary of names for the organization, with language codes as keys.
        prefLabels (dict):         A dictionary of preferred labels for the organization, with language codes as keys.
        descriptions (dict):       A dictionary of descriptions for the organization, with language codes as keys.
        homepage (str):            The URL to the organization's homepage.

    Returns:
        None
    """
    ODSN_ORGA = Namespace("https://opendata.swiss/id/organization/")
    LEGAL_FORM = Namespace("https://register.ld.admin.ch/i14y/concept/legalForm/")

    g = Graph()

    g.bind("dcterms", DCTERMS)
    g.bind("foaf", FOAF)

    orga_uri = ODSN_ORGA[slug]

    # Organization
    g.add((orga_uri, RDF.type, FOAF.Organization))
    g.add((orga_uri, RDF.type, ORG.Organization))
    g.add((orga_uri, DCTERMS.identifier, Literal(identifier)))

    if subAgentOf_slug:
        g.add((orga_uri, ORG.subOrganizationOf, ODSN_ORGA[subAgentOf_slug]))
    
    if classification_code:
        g.add((orga_uri, ORG.classification, LEGAL_FORM[classification_code])) 

    for lang, name in names.items():
        if name:
            g.add((orga_uri, FOAF.name, Literal(name, lang=lang)))

    for lang, prefLabel in prefLabels.items():
        if prefLabel:
            g.add((orga_uri, SKOS.prefLabel, Literal(prefLabel, lang=lang)))

    for lang, desc in descriptions.items():
        if desc:
            g.add((orga_uri, DCTERMS.description, Literal(desc, lang=lang)))

    if homepage:
        g.add((orga_uri, FOAF.homepage, URIRef(homepage)))
        g.add((URIRef(homepage), RDF.type, FOAF.Document))

    output_file = Path(ORGANIZATIONS_PATH) / f"{slug}.ttl"
    g.serialize(destination=output_file, format="turtle")
    logger.info(f"Successfully generated RDF triples and saved to '{output_file}'")


def generate_organization_json(
        slug: str,
        identifier: str,
        subAgentOf_slug: str,
        classification_code: str,
        names: dict,
        prefLabels: dict,
        descriptions: dict,
        homepage: str,
) -> None:
    """
    Generates a JSON document for an organization, for indexing in hub-search/elasticsearch.

    Args:
        slug (str):                The slug of the organization.
        identifier (str):          The identifier of the organization.
        subAgentOf_slug (str):     The slug of the parent organization, if any.
        classification_code (str): The classification code of the organization.
        names (dict):              A dictionary of names for the organization, with language codes as keys.
        prefLabels (dict):         A dictionary of preferred labels for the organization, with language codes as keys.
        descriptions (dict):       A dictionary of descriptions for the organization, with language codes as keys.
        homepage (str):            The URL to the organization's homepage.

    Returns:
        None
    """
    
    ODSN_ORGA = Namespace("https://opendata.swiss/id/organization/")
    LEGAL_FORM = Namespace("https://register.ld.admin.ch/i14y/concept/legalForm/")

    orga_uri = ODSN_ORGA[slug]

    filtered_descriptions = {
        lang: desc for lang, desc in descriptions.items() if desc is not None
    }
    filtered_names = {
        lang: name for lang, name in names.items() if name is not None
    }
    filtered_prefLabels = {
        lang: prefLabel for lang, prefLabel in prefLabels.items() if prefLabel is not None
    }

    classification = None
    if classification_code:
        legal_form_lookup = load_legal_form_lookup()
        classification = legal_form_lookup.get(classification_code)

        if classification is None:
            logger.warning(
                f"No legal form found in vocabulary for classification code '{classification_code}'."
            )
            
            classification = {
                "id": classification_code,
                "label": {},
                "resource": str(LEGAL_FORM[classification_code]),
            }

    
    output_file = Path(ORGANIZATIONS_PATH) / "es" / f"{slug}.json"
    with open(output_file, "w") as orga_file:
        json.dump({
            "id": slug,
            "identifier": identifier,
            "resource": orga_uri,
            # "sub_organization_of": ODSN_ORGA[subAgentOf_slug] if subAgentOf_slug else None,
            "classification": classification,
            "description": filtered_descriptions,
            "name": filtered_names,
            "pref_label": filtered_prefLabels,
            "homepage": homepage,
        }, orga_file, indent=2, ensure_ascii=False)

    logger.info(f"Successfully generated JSON and saved to '{output_file}'")


# run this locally and push the generated files to repo
def generate_pipe_and_catalogue_files(pipes: bool = True, catalogues: bool = True) -> None:
    """
    Fetches all harvesters from CKAN and generates corresponding
    pipe and catalogue metadata files.

    Args:
        pipes      (bool, optional):    If True, generates pipe definition files. Defaults to True.
        catalogues (bool, optional):    If True, generates catalogue metadata files. Defaults to True.
    """
    ckan_client = CkanClient()

    try:
        harversters = ckan_client.get_harvesters()
        harversters = [h for h in harversters if h["type"] not in ["geocat_harvester"]]
        logging.info(f"Collected {len(harversters)} harvester(s) from CKAN.")
    except HTTPError as e:
        logging.error(f"Failed to fetch harvester IDs from CKAN: {e}")
        return


    pipe_names = []

    for i, harverster in enumerate(harversters):
        id = harverster["id"]
        type = harverster["type"]

        logging.info(f"({i+1}/{len(harversters)}) Processing harvester ID {id} of type '{type}'...")

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

        url = details["url"].strip()
        if type=="geocat_harvester":
            url = url.split("?")[0]
        # encode for URL if not already encoded
        url = requests.utils.requote_uri(url)
        

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
                org_id = org_id,
                org_name=org_titles.get("en", "unknown_org"),
                catalogue=catalogue_name,
                title=details["title"],
                http_client=url,
                template_file=template_files.get(type, "meta_harvester/pipe-template.yaml")
            )
            pipe_names.append(details["name"])
    
    if pipes:
        generate_bulk_triggers(pipe_names)


# run this locally and push the generated files to repo
def generate_organizations() -> None:
    """
    Fetches all organizations from I14Y and generates corresponding
    organization metadata files.
    """
    i14y_client = I14YClient()

    try:
        organizations = i14y_client.get_organizations()
        logging.info(f"Collected {len(organizations)} organization(s) from I14Y.")
    except HTTPError as e:
        logging.error(f"Failed to fetch organizations from I14Y: {e}")
        return
    
    # dictionary to map from the id to the slug. used for populating the 'subAgentOf' fields
    id_to_slug = {org["id"]: org["slug"] for org in organizations}
    
    for i, organization in enumerate(organizations):
        slug = organization["slug"]

        logging.info(f"({i+1}/{len(organizations)}) Processing organization '{slug}'...")

        # expect only one parent organization. error if more than one.
        parent_orgs = organization.get("subAgentOf", [])
        if len(parent_orgs) > 1:
            logging.error(f"Organization '{slug}' ({organization["identifier"]}) has more than one parent organization: {parent_orgs}. Skipping.")
            continue
        elif len(parent_orgs) == 1:
            parent_org_id = parent_orgs[0].get("id")
            subAgentOf_slug = id_to_slug.get(parent_org_id)

        generate_organization_metadata(
            slug=slug,
            identifier=organization["identifier"],
            subAgentOf_slug=subAgentOf_slug if len(parent_orgs) == 1 else "",
            classification_code = (organization.get("classification") or {}).get("code", ""),
            names=to_dict(organization.get("name" or {})),
            prefLabels=to_dict(organization.get("prefLabel" or {})),
            descriptions=to_dict(organization.get("description" or {})),
            homepage=organization.get("homePage", "")
        )

        generate_organization_json(
            slug=slug,
            identifier=organization["identifier"],
            subAgentOf_slug=subAgentOf_slug if len(parent_orgs) == 1 else "",
            classification_code = (organization.get("classification") or {}).get("code", ""),
            names=to_dict(organization.get("name" or {})),
            prefLabels=to_dict(organization.get("prefLabel" or {})),
            descriptions=to_dict(organization.get("description" or {})),
            homepage=organization.get("homePage", "")
        )

def main()-> None:

    parser = argparse.ArgumentParser(description="Meta-Harvester CLI tool.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    parser_generate = subparsers.add_parser(
        "generate", help="Generate all pipes and catalogue files from CKAN."
    )
    parser_generate.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=True)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-pipes", help="Generate all pipe definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_pipe_and_catalogue_files, pipes=True, catalogues=False)

    parser_generate_pipes = subparsers.add_parser(
        "generate-all-catalogues", help="Generate all catalogue definition files from CKAN."
    )
    parser_generate_pipes.set_defaults(func=generate_pipe_and_catalogue_files, pipes=False, catalogues=True)

    parser_generate_organizations = subparsers.add_parser(
        "generate-all-organizations", help="Generate all organization definition files from I14Y."
    )
    parser_generate_organizations.set_defaults(func=generate_organizations)

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
