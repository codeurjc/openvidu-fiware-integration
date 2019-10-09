FROM node:8

COPY fiware-integration-manager /home/node/app

RUN ls /home/node/app

CMD cd /home/node/app; npm install; npm start
