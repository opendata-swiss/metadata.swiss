# Forward ports from
ssh -L 8081:localhost:8080 zazuko@lindas-next.zazukoians.org kubectl -n piveau port-forward svc/piveau-hub-repo 8080:8080
ssh -L 8090:localhost:8088 zazuko@lindas-next.zazukoians.org kubectl -n piveau port-forward svc/piveau-consus-scheduling 8088:8080
ssh -L 8080:localhost:8089 zazuko@lindas-next.zazukoians.org kubectl -n piveau port-forward svc/piveau-hub-ui 8089:8080


# Kill port forwards on remote server
ssh zazuko@lindas-next.zazukoians.org 'sudo kill -9 $(sudo lsof -t -i:8088)'
ssh zazuko@lindas-next.zazukoians.org 'sudo kill -9 $(sudo lsof -t -i:8089)'
ssh zazuko@lindas-next.zazukoians.org 'sudo kill -9 $(sudo lsof -t -i:8080)'
