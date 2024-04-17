# sso-live-coding by Daniel Garnier (https://www.youtube.com/watch?v=wP4TVTvYL0Y)
Live coding SSO
# Live coding SSO

This repo is a companion for a "live-coding Single Sign On" conference by Daniel Garnier.

## How to run the examples

1. Clone this repo
2. Copy `template.env` to `.env` at the root of the repo
3. Obtain a client_id and client_secret from Okta, and set the following redirect uris:
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
