from mitmproxy import http
import base64

BASIC_USER = "admin"
BASIC_PASS = "admin"

def request(flow: http.HTTPFlow) -> None:
    # # If the client sends Digest auth, replace with Basic
    # auth = flow.request.headers.get("authorization", "")
    # if auth.lower().startswith("digest"):
    # Replace with Basic Auth
    basic = f"{BASIC_USER}:{BASIC_PASS}"
    basic_b64 = base64.b64encode(basic.encode()).decode()
    flow.request.headers["authorization"] = f"Basic {basic_b64}"