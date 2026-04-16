import logging
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry
from .exceptions import NoRecords, NotFoundError

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

def requests_retry_session(
    retries=1,
    backoff_factor=0.3,
    status_forcelist=(500, 502, 504),
    session=None,
) -> requests.Session:

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

