from mitmproxy import http

def request(flow: http.HTTPFlow) -> None:
    flow.request.headers["authorization"] = f"Bearer super-secret-token"