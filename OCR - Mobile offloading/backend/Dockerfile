FROM node:argon

# Create app directory
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
RUN mkdir uploads
RUN mkdir uploads/src
RUN mkdir uploads/thumbs

# Install app dependencies
COPY package.json /usr/src/app/
RUN apt-get update
RUN apt-get -y install tesseract-ocr
RUN apt-get -y install ImageMagick
RUN npm install

# Bundle app source
COPY . /usr/src/app

EXPOSE 8443
CMD [ "npm", "start" ]
