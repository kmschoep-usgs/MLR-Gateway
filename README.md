# MLR-Gateway
[![Build Status](https://travis-ci.org/USGS-CIDA/MLR-Gateway.svg?branch=master)](https://travis-ci.org/USGS-CIDA/MLR-Gateway) [![Coverage Status](https://coveralls.io/repos/github/USGS-CIDA/MLR-Gateway/badge.svg?branch=master)](https://coveralls.io/github/USGS-CIDA/MLR-Gateway?branch=master)

Monitoring Location Gateway

## Running the Application
Copy the src/main/resources/application.yml file to you project root directory and change the substitution variables as needed.
Open a terminal window and navigate to the project's root directory.

Use the maven command ```mvn spring-boot:run``` to run the application.

It will be available at http://localhost:8080 in your browser.

Swagger API Documentation is available at http://localhost:8080/swagger-ui.html

ctrl-c will stop the application.

### localDev Profile
This application has two built-in Spring Profiles to aid with local development. 

The default profile, which is run when either no profile is provided or the profile name "default" is provided, is setup to require the database for Session storage.

The localDev profile, which is run when the profile name "localDev" is provided, is setup to require no external database - Sessions are stored internally.

## SSL Configuration
This application must communicate with the Water Auth Server for Authentication and this must be done over https. In order for this to work correctly the Water Auth Server SSL cert must be trusted by the Gateway. The following environemnt variables are used to confirue this store.

- **sslStorePassword_file** - The path to the mounted secret file of the SSL trust store password.

- **water_auth_cert_file** - The path to the mounted secret file of the Water Auth Server Public Cert.

## Spring Security Client Configuration
This is a secured application that must connect to a running instance of the Water Auth Server in order to work. The following environment variables are used to configure the connection to the water auth server via OAuth2.

- **oauthClientId** - The ID of the Client that has been configured in the Water Auth Server for this application.

- **oauthClientSecret** - The secret associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthClientSecret_file** - The path to the mounted secret file containing the secret associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthClientAccessTokenUri** - The OAuth2 Token URI that this application should connect to.

- **oauthClientAuthorizationUri** -  The OAuth2 Authorization URI that this application should connect to.

- **oauthResourceId** - The resource ID associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthResourceTokenKeyUri** - The OAuth2 Token Key URI that this application should connect to.

## Spring Security Client and Session Storage
This service by default stores session data within a database rather than within the application itself. This allows for multiple running instances of this service to share session information, thereby making the service stateless. 

When the application first starts it attempts to connect to the configured PostgreSQL database and run a set of initialization scripts to create the relevant tables if they don't already exist

The related environment variables are listed below:

- **dbConnectionUrl** - The full JDBC-qualified database URL that the application should connect to. Example: jdbc:postgresql://192.168.99.100:5432/mydb

- **dbUsername** - The username that should be used by the application when connecting to the database.

- **dbPassword** - The password that should be used by the application when connecting to the database.

- **dbPassword_file** - The path to the mounted secret file containing the password that should be used by the application when connecting to the database.

- **dbInitializerEnabled** - Whether or not the database initialization scripts should run on application startup. The default value is true.

## Substitution Variables
* mlrgateway_mlrServicePassword - password for the monitoring user (deprecated)
* mlrgateway_ddotServers - comma separated list of url(s) for the D dot Ingester Microservice
* mlrgateway_legacyTransformerServers - comma separated list of url(s) for the Legacy Transformer Microservice
* mlrgateway_legacyValidatorServers - comma separated list of url(s) for the Legacy Validator Microservice
* mlrgateway_legacyCruServers - comma separated list of url(s) for the Legacy CRU Microservice
* mlrgateway_fileExportServers - comma separated list of url(s) for the File Export Microservice
* mlrgateway_notificationServers - comma separated list of url(s) for the Notification Microservice
* mlrgateway_ribbonMaxAutoRetries - maximum number of times to retry connecting to a microservice
* mlrgateway_ribbonConnectTimeout - maximum milliseconds to wait for a connection to a microservice
* mlrgateway_ribbonReadTimeout - maximum milliseconds to wait for a response from a microservice
* mlrgateway_hystrixThreadTimeout - maximum milliseconds for a request to process
* mlrgateway_springFrameworkLogLevel - log level for org.springframework
