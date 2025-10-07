from .api_clients import CkanClient


def main():
    client = CkanClient()
    sources = client.get_geoharvesters()
    print(sources[-1])
    print(f"Collected {len(sources)} geoharvester(s)")
    return sources

if __name__ == "__main__":
    main()
