FROM node:8

COPY fiware-integration-manager /home/node/app

RUN ls /home/node/app

#CMD cd /home/node/app; export ORION_IP=$(getent ahostsv4 orion | awk '{print $1}' | head -1); sed -i 's/localhost/'"$ORION_IP"'/g' frontend/config.json; npm install; npm start;

CMD cd /home/node/app; sed -i 's/localhost/orion/g' frontend/config.json; npm install; npm start;
