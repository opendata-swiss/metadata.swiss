# filepath: ./strip_timeout.py
from mitmproxy import http

def request(flow: http.HTTPFlow) -> None:
    if "timeout" in flow.request.query:
        del flow.request.query["timeout"]