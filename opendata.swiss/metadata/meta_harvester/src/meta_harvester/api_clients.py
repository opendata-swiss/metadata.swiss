import logging
import os
from pathlib import Path

import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

from .exceptions import NoRecords, NotFoundError

CATALOGUES_PATH = Path(os.getenv("CATALOGUES_PATH", "../piveau_catalogues"))
PIPES_PATH = Path(os.getenv("PIPES_PATH", "../piveau_pipes"))

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def requests_retry_session(
    retries=1,
    backoff_factor=0.3,
    status_forcelist=(500, 502, 504),
    session=None,
):

    session = session or requests.Session()
    retry = Retry(
        total=retries,
        read=retries,
        connect=retries,
        backoff_factor=backoff_factor,
        status_forcelist=status_forcelist,
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("http://", adapter)
    session.mount("https://", adapter)

    return session


class CkanClient:

    HARVESTER_LIST_URL = "https://ckan.opendata.swiss/api/3/action/harvest_source_list"
    HARVESTER_SHOW_URL = "https://ckan.opendata.swiss/api/3/action/harvest_source_show"
    ORGANIZATION_SHOW_URL = "https://ckan.opendata.swiss/api/3/action/organization_show"
    GEOHARVESTER_FLAG = "geocat_harvester"

    def get_org_id_for_harvester(self, harvester_id: str) -> str | None:
        """
        Fetches organization id for a specific harvester.

        Args:
            harvester_id (str): The ID of the CKAN harvester.

        Returns:
            str | None: The organization ID, or None if not found or on error.
        """
        session = requests_retry_session()
        request = {"id": harvester_id}

        try:
            response = session.post(self.HARVESTER_SHOW_URL, params=request, timeout=10)
            response.raise_for_status()
            data = response.json()

        except requests.exceptions.RequestException as e:
            logger.error(f"Error fetching harvester '{harvester_id}' from {self.HARVESTER_SHOW_URL}: {e}")
            if e.response is not None:
                logger.error(f"Server response: {e.response.text}")
            return None

        result = data.get("result")
        if not result:
            logger.warning(f"No 'result' in API response for harvester '{harvester_id}'.")
            return None

        org_id = result.get("organization").get("name") if result.get("organization") else None
        if not org_id:
            logger.warning(f"No '[organization][name]' object found for harvester '{harvester_id}'.")
            return None

        return org_id


    def get_organization_details(self, organization_id: str) -> dict | None:
        """
        Fetches details for an organization by its ID.

        Args:
            organization_id (str): The ID of the CKAN organization.

        Returns:
            dict | None: A dictionary with the organization's details, or None on error.
        """
        session = requests_retry_session()
        params = {"id": organization_id}

        try:
            response = session.post(self.ORGANIZATION_SHOW_URL, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
        except requests.exceptions.RequestException as e:
            url = e.request.url if e.request else self.ORGANIZATION_SHOW_URL
            logger.error(f"Error fetching organization '{organization_id}' from {url}: {e}")
            if e.response is not None:
                logger.error(f"Server response: {e.response.text}")
            return None

        result = data.get("result")
        if not result:
            logger.warning(f"No 'result' in API response for organization '{organization_id}'.")
            return None

        return result


    def get_harvesters(self) -> list[dict[str, str]]:
        """Get all harvest sources from ckan.opendata.swiss

        Returns
            list[dict[str, str]]:	   CKAN harvest sources
        """

        session = requests_retry_session()
        request = {}

        response = session.post(self.HARVESTER_LIST_URL, params=request)
        response.raise_for_status()
        response = response.json()

        result = response.get("result")
        if not result:
            raise NotFoundError()

        result = response["result"]

        if len(result) == 0:
            raise NoRecords()

        return result

    def get_geoharvesters_ids(self) -> list[str]:
        """Get all geoharvesters from ckan.opendata.swiss

        Returns
            list[str]:	   CKAN geoharvesters
        """

        sources = self.get_harvesters()
        geoharvesters = [
            source["id"]
            for source in sources
            if source["type"] == self.GEOHARVESTER_FLAG
        ]
        return geoharvesters

    def get_harvester_details_by_id(self, harvester_id: str) -> str:
        """Get harvester details, incl. organization, by its id

        Args:
            harvester_id (str):	   CKAN harvester id

        Returns
            dict[str,str]:	       CKAN harvester details
        """

        session = requests_retry_session()
        request = {"id": harvester_id}
        # if time.time() < self.last_request + 1:
        #    time.sleep(1)
        # self.last_request = time.time()

        response = session.post(self.HARVESTER_SHOW_URL, params=request)
        response.raise_for_status()
        response = response.json()

        result = response.get("result")

        if not result:
            raise NotFoundError()

        return result


class PiveauRunClient:
    """
    A client for interacting with the piveau-scheduling 'Run' endpoints.
    """
    CONSUS_SCHEDULING_ENDPOINT = os.getenv("CONSUS_SCHEDULING_ENDPOINT", "http://localhost:8090")

    def list_runs(self, run_filter: list[str] = None) -> list[dict]:
        """
        Lists current or recent runs, with an option to filter by status.

        Args:
            run_filter (list[str], optional): A list of statuses to filter by.
                                            Valid values are: "active", "canceled", "failed", "finished".
                                            Defaults to None.

        Returns:
            list[dict]: A list of run status objects.
        """
        url = f"{self.CONSUS_SCHEDULING_ENDPOINT}/runs"
        params = {}
        if run_filter:
            params["filter"] = run_filter

        session = requests_retry_session()
        try:
            logger.info(f"Listing runs from {url} with filter: {run_filter}")
            response = session.get(url, params=params, timeout=60)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Error listing runs from {url}: {e}")
            if e.response is not None:
                logger.error(f"Server response: {e.response.text}")
            return []

class PiveauClient:

    HUB_REPO_ENDPOINT = os.getenv("HUB_REPO_ENDPOINT", "http://localhost:8081")
    CONSUS_SCHEDULING_ENDPOINT = os.getenv("CONSUS_SCHEDULING_ENDPOINT", "http://localhost:8090")
    API_KEY = os.getenv("PIVEAU_HUB_API_KEY", "secret-hub-api-key") #TODO: this is not read from .env

    def __init__(self):
        self._catalogues: set[str] = set()
        self.run_client = PiveauRunClient()
        self.max_concurrent_runs = int(os.getenv("MAX_CONCURRENT_RUNS", "5"))

    @property
    def catalogues(self) -> set[str]:
        """
        Provides a cached list of catalogues.
        Fetches from the API only on the first call or after a refresh.
        """
        if len(self._catalogues) == 0 or self._catalogues is None:
            logger.info("Catalogue cache is empty. Fetching from API...")
            self.refresh_catalogues()
        return self._catalogues

    def refresh_catalogues(self) -> None:
        """Forces a refresh of the catalogue cache from the API."""

        url = f"{self.HUB_REPO_ENDPOINT}/catalogues"

        headers = {"X-API-Key": self.API_KEY}
        params = {"valueType": "identifiers"}

        session = requests_retry_session()
        response = session.get(url, headers=headers, params=params, timeout=30)
        response.raise_for_status()

        api_catalogues = response.json()
        logger.info(f"API returned {len(api_catalogues)} catalogues.")
        self._catalogues = set([url.split('/')[-1] for url in api_catalogues])

        logger.info(f"Catalogue cache refreshed. Found {len(self._catalogues)} catalogues.")


    def create_catalogue(self, name: str, metadata_file: str, recreate: bool=False) -> None:
        """
        Uploads a catalogue's metadata to the piveau-hub-repo.

        Args:
            name (str): The name of the catalogue, used to build the URL (e.g., "stadt-biel-geocat").
            metadata_file (str): The path to the .ttl file containing the metadata.
        """

        url = f"{self.HUB_REPO_ENDPOINT}/catalogues/{name}"

        headers = {"X-API-Key": self.API_KEY, "Content-Type": "text/turtle"}

        with open(metadata_file, "r", encoding="utf-8") as f:
            data = f.read()


        if recreate and name in self.catalogues:
            logger.info(f"Catalogue '{name}' already exists. Deleting it before update.")
            self.delete_catalogues([name])

        session = requests_retry_session()
        response = session.put(url, headers=headers, data=data)
        response.raise_for_status()
        logger.info(
            f"Successfully created/updated catalogue '{name}'. Status: {response.status_code}"
        )

        if name not in self.catalogues:
            self._catalogues.add(name)


    def delete_catalogues(self, names: list[str]) -> None:
        """
        Deletes catalogues from the piveau-hub-repo.
        If no names are provided, all catalogues will be deleted.

        Args:
            names (list[str], optional): A list of catalogue names to delete. Defaults to None.
        """

        preexisting_catalogues = self.catalogues.copy()
        if len(names) == 0:
            names = self.catalogues.copy()
            logger.info("No catalogue names provided. Deleting all catalogues.")
        else:
            names = set(names)


        headers = {"X-API-Key": self.API_KEY}
        session = requests_retry_session()

        for name in names:
            if name not in preexisting_catalogues:
                logger.info(f"Catalogue '{name}' does not exist. Skipping deletion.")
                continue
            url = f"{self.HUB_REPO_ENDPOINT}/catalogues/{name}"
            try:
                response = session.delete(url, headers=headers)
                response.raise_for_status()
                logger.info(f"Successfully deleted catalogue '{name}'. Status: {response.status_code}"
                )
                if self._catalogues and name in self._catalogues:
                    self._catalogues.remove(name)
            except requests.exceptions.RequestException as e:

                if e.response and e.response.status_code == 404:
                    if self._catalogues and name in self._catalogues:
                        self._catalogues.remove(name)
                    logger.info(f"Catalogue '{name}' not found (404), skipping.")
                else:
                    logger.error(f"Error during API request to {url}: {e}")
                    logger.error(f"Failed to delete catalogue '{name}'. Status: {e.response}")


                if e.response is not None:
                    logger.error(f"Server response: {e.response.text}")

                raise e


    def create_catalogues(self, catalogue_names: list[str] | None = None, recreate: bool = False) -> None:
        """
        Creates or recreates one or more catalogues in the piveau-hub-repo.
        For each name, it derives the metadata file path from the CATALOGUES_PATH.
        If a catalogue exists, it is deleted first.
        """

        if not catalogue_names:
            catalogue_dir = CATALOGUES_PATH
            if not catalogue_dir.is_dir():
                logger.error(f"Catalogues directory not found: {CATALOGUES_PATH}")
                return
            catalogue_files = list(catalogue_dir.glob("*.ttl"))
            if not catalogue_files:
                logger.warning(f"No catalogue files (.ttl) found in '{CATALOGUES_PATH}'.")
                return
            catalogue_names = [f.stem for f in catalogue_files]


        for name in catalogue_names:
            metadata_file = Path(CATALOGUES_PATH) / f"{name}.ttl"
            if not metadata_file.is_file():
                logger.error(
                    f"Metadata file for '{name}' not found at '{metadata_file}'. Skipping."
                )
                continue

            logger.info(f"Creating catalogue '{name}' from file '{metadata_file}'")

            self.create_catalogue(name=name, metadata_file=str(metadata_file), recreate=recreate)


    def upload_pipe(self, pipe_name: str) -> None:
        """
        Uploads a pipe definition to the piveau-consus-scheduling service.

        Args:
            pipe_name (str): The name of the pipe, which corresponds to the YAML file name (without extension).
        """
        pipe_file = PIPES_PATH / f"{pipe_name}.yaml"
        if not pipe_file.is_file():
            logger.error(f"Pipe definition file not found at '{pipe_file}'. Aborting upload.")
            return

        url = f"{self.CONSUS_SCHEDULING_ENDPOINT}/pipes/{pipe_name}"
        logger.info(f"Uploading pipe definition for '{pipe_name}' from '{pipe_file}' to '{url}'")

        headers = {"X-API-Key": self.API_KEY}

        with open(pipe_file, 'rb') as f:
            files = {'data': (pipe_file.name, f, 'application/x-yaml')}

            session = requests_retry_session()

            response = session.put(url, headers=headers, data=files, timeout=30)
            response.raise_for_status()

            if response.status_code == 201:
                logger.info(f"Successfully created pipe '{pipe_name}'.")
            elif response.status_code == 204:
                logger.info(f"Successfully updated pipe '{pipe_name}'.")

            else:
                logger.warning(f"Unexpected status code {response.status_code} while uploading pipe '{pipe_name}'.")

    def trigger_pipe(self, pipe_name: str) -> None:
        """
        Triggers a specific pipe to run immediately.

        Args:
            pipe_name (str): The name of the pipe to trigger.
        """


        url = f"{self.CONSUS_SCHEDULING_ENDPOINT}/pipes/{pipe_name}/triggers/immediateTrigger"
        headers = {"Content-Type": "application/json"}
        payload = {"status": "enabled", "id": "immediateTrigger"}

        session = requests_retry_session()
        response = session.put(url, headers=headers, json=payload)
        response.raise_for_status()

        logger.info(f"Successfully triggered pipe '{pipe_name}'. Status: {response.status_code}")
