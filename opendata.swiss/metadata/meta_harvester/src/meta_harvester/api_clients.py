import os

import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

from .exceptions import NoRecords, NotFoundError


def requests_retry_session(
    retries=3,
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
    GEOHARVESTER_FLAG = "geocat_harvester"

    def get_harvesters(self) -> list[dict[str, str]]:
        """Get all harvest sources from ckan.opendata.swiss

        Returns
            list[dict[str, str]]:	   CKAN harvest sources
        """

        session = requests_retry_session()
        request = {}
        # if time.time() < self.last_request + 1:
        #    time.sleep(1)
        # self.last_request = time.time()

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


class PiveauClient:

    HUB_REPO_ENDPOINT = os.getenv("HUB_REPO_ENDPOINT", "http://localhost:8081")
    PIVEAU_PIPES_ENDPOINT = os.getenv("PIVEAU_PIPES_ENDPOINT", "http://localhost:8090")
    API_KEY = os.getenv("API_KEY_HUB", "yourRepoApiKey")

    def create_catalogue(self, name: str, metadata_file: str) -> None:
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

        session = requests_retry_session()

        response = session.put(url, headers=headers, data=data)
        response.raise_for_status()
        print(
            f"Successfully created/updated catalogue '{name}'. Status: {response.status_code}"
        )

    def delete_catalogue(self, name: str) -> None:
        """
        Deletes a catalogue from the piveau-hub-repo.

        Args:
            name (str): The name of the catalogue to delete.
        """

        url = f"{self.HUB_REPO_ENDPOINT}/catalogues/{name}"

        headers = {"X-API-Key": self.API_KEY}

        session = requests_retry_session()
        response = session.delete(url, headers=headers)
        response.raise_for_status()

        print(
            f"Successfully deleted catalogue '{name}'. Status: {response.status_code}"
        )

    def trigger_pipe(self, pipe_name: str) -> None:
        """
        Triggers a specific pipe to run immediately.

        Args:
            pipe_name (str): The name of the pipe to trigger.
        """


        url = f"{self.PIVEAU_PIPES_ENDPOINT}/pipes/{pipe_name}/triggers/immediateTrigger"
        headers = {"Content-Type": "application/json"}
        payload = {"status": "enabled", "id": "immediateTrigger"}

        session = requests_retry_session()
        response = session.put(url, headers=headers, json=payload)
        response.raise_for_status()

        print(f"Successfully triggered pipe '{pipe_name}'. Status: {response.status_code}")
