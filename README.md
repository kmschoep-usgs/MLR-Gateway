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

## Spring Security Client and Session Storage
This service by default stores session data within a database rather than within the application itself. This allows for multiple running instances of this service to share session information, thereby making the service stateless. 

When the application first starts it attempts to connect to the configured database and run a set of initialization scripts to create the relevant tables if they don't already exist

The related environment variables are listed below:

- **dbDriverClassName** - The driver class to use when connecting to the configured database. The default driver is PostgreSQL using the org.postgresql.Driver class. In order to use other drivers their JAR dependencies would need to be included with or injected into the application JAR. 

- **dbSchemaType** - The type of database that the service will connect to. This informs the initializer as to which initialization script to use. The default value is postgresql.

- **dbConnectionUrl** - The full JDBC-qualified database URL that the application should connect to. Example: jdbc:postgresql://192.168.99.100:5432/mydb

- **dbUsername** - The username that should be used by the application when connecting to the database.

- **dbPassword** - The password that should be used by the application when connecting to the database.

- **dbInitializerEnabled** - Whether or not the database initialization scripts should run on application startup. The default value is true.

## Using Docker
To build the image you will need to provide the location of the jar within 
https://cida.usgs.gov/artifactory/mlr-maven/gov/usgs/wma/mlrgateway as follows:
``` 
% docker build --build-arg=0.1-SNAPSHOT/mlrgateway-0.1-SNAPSHOT.jar .
```

To run the image, you will need to provide as environment variables the substitution variables in the application.yml. The application
will be available on part 8080 within the container.

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
