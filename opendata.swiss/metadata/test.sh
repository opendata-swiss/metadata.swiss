curl -i -u "piveau-consus-scheduling:ohGhaer6bai7quae" https://piveau-consus-scheduling-ln.zazukoians.org/runs
https://piveau-consus-scheduling-ln.zazukoians.org/catalogues


curl -i -X PUT \
     -H "Content-Type: application/json" \
     -u "piveau-consus-scheduling:ohGhaer6bai7quae" \
     -d '{"status": "enabled", "id": "immediateTrigger"}' \
     "https://piveau-consus-scheduling-ln.zazukoians.org/pipes/aargau-kt-geocat-harvester/triggers/immediateTrigger"


curl -i -X GET \
     -u "piveau-consus-scheduling:ohGhaer6bai7quae" \
     "https://piveau-consus-scheduling-ln.zazukoians.org/triggers"


curl -v -X PUT \
  -u "piveau-consus-scheduling:ohGhaer6bai7quae" \
  --data "@piveau_pipes/babs-geocat-harvester.json" \
  -H "Content-Type: application/json" \
  -s -o /dev/null -w "%{http_code}" \
  https://piveau-consus-scheduling-ln.zazukoians.org/pipes
