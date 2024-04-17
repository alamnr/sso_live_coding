# sso-live-coding by Daniel Garnier 
Live coding SSO
# Live coding SSO

This repo is a companion for a "live-coding Single Sign On" conference.

## How to run the examples

1. Clone this repo
2. Copy `template.env` to `.env` at the root of the repo
3. Obtain a client_id and client_secret from Google, and set the following redirect uris:
    1. `http://localhost:8080/oauth2/callback` (for java)
    
4. Save the credentials in your `.env` file


### java

Using JDK 17+, navigate to the `resource_server\server` directory and  start the app with maven:


```

mvn spring-boot:run
open http://localhost:8081
```
Then navigate to the `demo_sso` directory and  start the app with maven:

```

mvn spring-boot:run
open http://localhost:8080
```

### python

Using [Poetry](https://python-poetry.org/), navigate to the `python` directory,
and run the app with `flask`:

```
cd python
poetry install
poetry run flask run
open http://localhost:5000
```


### nodejs

Using node 18, navigate to the `javascript` directory, and run with `start` (or `live`):

```
cd javascript
npm install
npm run start
open http://localhost:3000
```
