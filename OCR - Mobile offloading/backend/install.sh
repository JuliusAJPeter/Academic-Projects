echo "======>>> Starting installation: Setting variables..."
sleep 1
gcloud config set compute/zone europe-west1-c
export PROJECT_ID="mcc-2016-g09-p2"

echo "======>>> Building a Docker image..."
sleep 1
docker build -t gcr.io/$PROJECT_ID/server:v1 .
sleep 1
gcloud docker push gcr.io/$PROJECT_ID/server:v1

echo "======>>> Creating the cluster..."
sleep 1
gcloud container \
  --project "mcc-2016-g09-p2" \
  clusters create "server-cluster" \
  --zone "europe-west1-c" \
  --machine-type "n1-standard-1" \
  --num-nodes "3" \
  --network "default"

gcloud config set container/use_client_certificate True
gcloud container clusters get-credentials server-cluster

echo "======>>> Cloning Sidecar..."
sleep 1
git clone https://github.com/leportlabs/mongo-k8s-sidecar.git

echo "======>>> Creating controller & service for mongoDB replicas..."
sleep 1

cd mongo-k8s-sidecar/example
make add-replica DISK_SIZE=200GB ZONE=europe-west1-c
make add-replica DISK_SIZE=200GB ZONE=europe-west1-c
make add-replica DISK_SIZE=200GB ZONE=europe-west1-c

cd ../..

echo "======>>> Waiting for the replicas to initiate..."
sleep 120

echo "======>>> Creating controller & service for the server..."
sleep 1
kubectl create -f web_controller.yml
kubectl create -f web_service.yml

echo "======>>> Waiting for external IP..."
sleep 45
kubectl get services

# Get external IP
IP=$(kubectl get services | grep '^web' | sed -E "s/.*\b([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+).*?/\\1/")

echo "Server running on IP $IP"
