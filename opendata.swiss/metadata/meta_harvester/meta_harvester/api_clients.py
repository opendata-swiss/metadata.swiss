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

class I14YClient:

    AGENT_LIST_URL = "https://input-backend.i14y.c.bfs.admin.ch/api/Agent"

    def get_organizations(self) -> list[dict]:
        """Get all organizations from input-backend.i14y.c.bfs.admin.ch

        Returns
            list[dict]: I14Y organizations
        """

        session = requests_retry_session()
        request = {}
        response = session.get(self.AGENT_LIST_URL, params=request)
        response.raise_for_status()
        organizations = response.json()

        # `subAgentOf` is intentionally not populated anymore in the list response.
        # Reconstruct inverse parent links from each parent's `subAgents` collection.
        id_to_org = {
            org.get("id"): org
            for org in organizations
            if isinstance(org, dict) and org.get("id")
        }
        inferred_edges = 0
        unresolved_children = 0

        for parent in organizations:
            if not isinstance(parent, dict):
                continue

            parent_id = parent.get("id")
            if not parent_id:
                continue

            sub_agents = parent.get("subAgents") or []
            if not isinstance(sub_agents, list):
                logger.warning(
                    f"Organization '{parent_id}' has non-list 'subAgents': {type(sub_agents).__name__}"
                )
                continue

            parent_ref = {"id": parent_id}
            parent_name = parent.get("name")
            if isinstance(parent_name, dict):
                parent_ref["name"] = parent_name

            for child_ref in sub_agents:
                if not isinstance(child_ref, dict):
                    continue

                child_id = child_ref.get("id")
                if not child_id:
                    continue

                child_org = id_to_org.get(child_id)
                if not child_org:
                    unresolved_children += 1
                    continue

                existing = child_org.get("subAgentOf")
                if existing is None:
                    normalized = []
                elif isinstance(existing, list):
                    normalized = [item for item in existing if isinstance(item, dict)]
                elif isinstance(existing, dict):
                    normalized = [existing]
                else:
                    normalized = []

                if not any(item.get("id") == parent_id for item in normalized):
                    normalized.append(parent_ref.copy())
                    inferred_edges += 1

                child_org["subAgentOf"] = normalized

        logger.info(
            f"Inferred {inferred_edges} parent link(s) from 'subAgents' ({unresolved_children} unresolved child reference(s))."
        )

        # identifiers look like this: "CH_ZAS", "CHE-229.707.417", ...
        # we create a URL friendly slug for each identifier, e.g. "CH_ZAS" -> "ch-zas", "CHE-229.707.417" -> "che-229-707-417"
        # TODO: this is temporary solution until I14Y provides a slug field in the API response
        for org in organizations:
            identifier = org.get("identifier")
            if identifier:
                slug = identifier.lower().replace("_", "-").replace(".", "-")
                org["slug"] = slug
            else:
                logger.warning(f"Organization with missing 'identifier' field: {org}")

        return organizations