METADATA_BASE_URL="http://metadata.google.internal/computeMetadata/v1/instance"
METADATA_FLAVOR="Metadata-Flavor: Google"
METADATA_EXT_IP_URL="${METADATA_BASE_URL}/network-interfaces/0/access-configs/0/external-ip"
METADATA_NAME_URL="${METADATA_BASE_URL}/hostname"

EXT_IP=$(curl "$METADATA_EXT_IP_URL" -H "$METADATA_FLAVOR" -s)
HOSTNAME=$(curl "$METADATA_NAME_URL" -H "$METADATA_FLAVOR" -s)
NAME=${HOSTNAME%%.*}
echo "Name: $NAME, Ext.IP: $EXT_IP"

# start vnc server (for options see ~/.vnc/xstartup )
vncserver -name $NAME

# start noVNC tunnelling (for html vnc viewer)
~/Documents/noVNC/utils/launch.sh --vnc localhost:5901 --listen 6080
