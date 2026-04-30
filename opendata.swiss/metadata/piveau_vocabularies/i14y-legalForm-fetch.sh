#!/bin/sh

curl \
    -o i14y-legalForm.nt \
    -H "Accept: application/n-triples" \
    --data-urlencode query@i14y-legalForm.rq \
    https://lindas.admin.ch/query