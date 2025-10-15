
docker compose -f docker-compose-consus.yml down -v
docker compose -f docker-compose-hub.yml down -v


docker compose -f docker-compose-consus.yml up -d --build
docker compose -f docker-compose-hub.yml up -d

docker compose -f docker-compose-consus.yml logs > docker-compose-logs.log

#docker compose --file docker-compose-consus.yml logs -f | grep ERROR
