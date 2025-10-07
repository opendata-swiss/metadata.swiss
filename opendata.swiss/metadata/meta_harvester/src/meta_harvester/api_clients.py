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

    BASE_URL = "https://ckan.opendata.swiss/api/3/action/harvest_source_list"
    GEOHARVESTER_FLAG = "geocat_harvester"

    def get_harvest_sources(self) -> list[dict[str, str]]:

        """Get all harvest sources from ckan.opendata.swiss

        Returns
            list[dict[str, str]]:	   CKAN harvest sources
        """

        session = requests_retry_session()
        request = {}
        #if time.time() < self.last_request + 1:
        #    time.sleep(1)
        #self.last_request = time.time()

        response = session.post(self.BASE_URL, params=request)
        response.raise_for_status()
        response = response.json()

        if len(response) == 0:
            raise NotFoundError()

        result = response["result"]

        if len(result) == 0:
            raise NoRecords()

        return result

    def get_geoharvesters(self) -> list[dict[str, str]]:
        """Get all geoharvesters from ckan.opendata.swiss

        Returns
            list[dict[str, str]]:	   CKAN geoharvesters
        """

        sources = self.get_harvest_sources()
        geoharvesters = [
            source for source in sources if source["type"] == self.GEOHARVESTER_FLAG
        ]
        return geoharvesters
