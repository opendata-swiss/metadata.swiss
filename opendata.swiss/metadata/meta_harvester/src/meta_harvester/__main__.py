import re
from pathlib import Path

import yaml

from .api_clients import CkanClient

TEMPLATE_FILE = "src/meta_harvester/pipe-template.yaml"
PIPES_PATH = "../piveau_pipes"


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

    temp_name = re.sub(r"\s*-\s*", "-", name)
    slugified_name = re.sub(r"\s+", "-", temp_name.lower())

    data["header"]["id"] = id
    data["header"]["name"] = slugified_name
    data["header"]["title"] = title

    data["body"]["segments"][0]["body"]["config"]["address"] = http_client
    data["body"]["segments"][0]["body"]["config"]["catalogue"] = slugified_name

    output_file = Path(output_path) / f"{slugified_name}.yaml"

    with open(output_file, "w") as file:
        yaml.dump(data, file, sort_keys=False, indent=2)

    print(f"Successfully generated '{output_file}'")


def main():
    client = CkanClient()
    sources = client.get_geoharvesters()
    print(sources[-1])
    print(f"Collected {len(sources)} geoharvester(s)")

    for source in sources[0:2]:
        id = source["id"]
        name = source["title"]
        title = source["title"]
        http_client = source["url"]

        generate_pipe(id=id, name=name, title=title, http_client=http_client)
    return sources


# --- Example Usage ---
if __name__ == "__main__":

    main()
