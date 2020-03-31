# MLR-Gateway

[![Build Status](https://travis-ci.org/USGS-CIDA/MLR-Gateway.svg?branch=master)](https://travis-ci.org/USGS-CIDA/MLR-Gateway) [![Coverage Status](https://coveralls.io/repos/github/USGS-CIDA/MLR-Gateway/badge.svg?branch=master)](https://coveralls.io/github/USGS-CIDA/MLR-Gateway?branch=master)

Monitoring Location Gateway

## Service Description

This service is the central componenet of the MLR system which handles conducting the various MLR workflows and executing any calls to the other MLR microservices. Any inter-service communication between the MLR services should be routed through the Gateway. The Gateway also serves as the single entrypoint into the microservices for user/client connections, and as a result it also handles generating auth tokens for users and properly routing connections to the correct downstream services.

The UI is currently embedded within the Gateway aplication due to the way that authentication is handled by the Gateway. There are plans in the future to change this and at that point the UI would be extracted from the Gateway and moved into its own separate microservice.

The Gateway provides two types of business logic API endpoints: Workflows and Service Proxies.

Workflows are the primary focus of the Gateway and makeup the vast majority of the business logic within the Gateway itself. The workflows execute the various information management functions of MLR including adding new sites, updating existing sites, and exporting sites to other hosts. Each of these processes includes a set of required steps that must be completed and the Gateway handles performing these steps by creating requests to downstream services and handling their responses. The Gateway also handles the creation of the output JSON report that is generated for each workflow to illustrate the executed steps to the users.

Service Proxies are the other function of the Gateway. The Gateway is the only service in MLR that is exposed to the full USGS network rather than just the other services within CHS. As a result any user interaction with other services must be proxied through the Gateway. This gives the benefit of a central location for handling user authentication and converting any service errors into more readable errors for users. Service proxies are used whenever a user needs to directly access a method of a downstream MLR service. At this point the only service proxy is for retrieval of sites from the MLR Legacy CRU service. This service proxy allows users to retrieve sites from MLR through the Gateway but without running a full Gateway workflow. There is no business logic written into the Gateway for service proxies, and requests are simply proxied from the Gateway to the downstream service.

The Gateway service, in addition to its connections to all downstream MLR services, also has a direct connection to the MLR Database (one of only two services to have this). The Gateway uses the MLR Database to persist user sessions, which allows multiple instances of the MLR Gateway to share their user sessions and prevent auth issues when a user authenticates with one instance of the Gateway and then has a request load-balanced to another instance.

Available API methods (Workflows, Service Proxies, and other utility endpoints) can be viewed in detail in the Swagger API documentation.

## Running the Application (non-Docker)

Copy the src/main/resources/application.yml file to you project root directory and change the substitution variables as needed.
Open a terminal window and navigate to the project's root directory.

Use the maven command ```mvn spring-boot:run``` to run the application.

It will be available at <https://localhost:8443> in your browser.

Swagger API Documentation is available at <https://localhost:8443/swagger-ui.html>

ctrl-c will stop the application.

### localDev Profile

This application has two built-in Spring Profiles to aid with local development.

The default profile, which is run when either no profile is provided or the profile name "default" is provided, is setup to require the database for Session storage.

The localDev profile, which is run when the profile name "localDev" is provided, is setup to require no external database - Sessions are stored internally.

### SSL Configuration (non-Docker)

This application must communicate with the Water Auth Server for Authentication and this must be done over https. In order for this to work correctly the Water Auth Server SSL cert must be trusted by the Gateway. The following environment variables are used to configure this store.

- **sslStorePassword_file** - The path to the mounted secret file of the SSL trust store password.

- **water_auth_cert_file** - The path to the mounted secret file of the Water Auth Server Public Cert.

### Spring Security Client Configuration

This is a secured application that must connect to a running instance of the Water Auth Server in order to work. The following environment variables are used to configure the connection to the water auth server via OAuth2.

- **oauthClientId** - The ID of the Client that has been configured in the Water Auth Server for this application.

- **oauthClientSecret** - The secret associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthClientSecret_file** - The path to the mounted secret file containing the secret associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthClientAccessTokenUri** - The OAuth2 Token URI that this application should connect to.

- **oauthClientAuthorizationUri** -  The OAuth2 Authorization URI that this application should connect to.

- **oauthResourceId** - The resource ID associated with the Client that has been configured in the Water Auth Server for this application.

- **oauthResourceTokenKeyUri** - The OAuth2 Token Key URI that this application should connect to.

### Spring Security Client and Session Storage

This service by default stores session data within a database rather than within the application itself. This allows for multiple running instances of this service to share session information, thereby making the service stateless.

When the application first starts it attempts to connect to the configured PostgreSQL database and run a set of initialization scripts to create the relevant tables if they don't already exist

The related environment variables are listed below:

- **dbConnectionUrl** - The full JDBC-qualified database URL that the application should connect to. Example: jdbc:postgresql://192.168.99.100:5432/mydb

- **dbUsername** - The username that should be used by the application when connecting to the database.

- **dbPassword** - The password that should be used by the application when connecting to the database.

- **dbPassword_file** - The path to the mounted secret file containing the password that should be used by the application when connecting to the database.

- **dbInitializerEnabled** - Whether or not the database initialization scripts should run on application startup. The default value is true.

## Substitution Variables

- mlrgateway_mlrServicePassword - password for the monitoring user (deprecated)
- mlrgateway_ddotServers - comma separated list of url(s) for the D dot Ingester Microservice
- mlrgateway_legacyTransformerServers - comma separated list of url(s) for the Legacy Transformer Microservice
- mlrgateway_legacyValidatorServers - comma separated list of url(s) for the Legacy Validator Microservice
- mlrgateway_legacyCruServers - comma separated list of url(s) for the Legacy CRU Microservice
- mlrgateway_fileExportServers - comma separated list of url(s) for the File Export Microservice
- mlrgateway_notificationServers - comma separated list of url(s) for the Notification Microservice
- mlrgateway_ribbonMaxAutoRetries - maximum number of times to retry connecting to a microservice
- mlrgateway_ribbonConnectTimeout - maximum milliseconds to wait for a connection to a microservice
- mlrgateway_ribbonReadTimeout - maximum milliseconds to wait for a response from a microservice
- mlrgateway_hystrixThreadTimeout - maximum milliseconds for a request to process
- mlrgateway_springFrameworkLogLevel - log level for org.springframework

## Running the Application via Docker

This application can be run locally using the docker container built during the build process or by directly building and running the application JAR. The included `docker-compose` file has 3 profiles to choose from when running the application locally:

1. mlr-gateway: This is the default profile which runs the application as it would be in our cloud environment. This is not recommended for local development as it makes configuring connections to other services running locally on your machine more difficult.
2. mlr-gateway-local-dev: This is the profile which runs the application as it would be in the aqcu-local-dev project, and is configured to make it easy to replace the mlr-gateway-service instance in the local-dev project with this instance. It is run the same as the `mlr-gateway` profile, except it uses the docker host network driver.
3. mlr-gateway-debug: This is the profile which runs the application exactly the same as `mlr-gateway-local-dev` but also enables remote debugging for the application and opens up port 8000 into the container for that purpose.

### Setting up SSL

This application is configured to run over HTTPS and thus requires SSL certificates to be setup before it can be run via Docker. When running this container alone and not with an MLR Local Dev setup SSL certificates can be configured easily by simply running the included `create_keys.sh` script in the `docker/certificates` directory.

When intending to run this application alongside other MLR service running from the MLR Local Dev project you should use the certificate files generated by the MLR Local Dev project. This is important because in order for the MLR Local Dev services to connect to this service they must trust the certificate it is serving, which is most easily accomplished locally by using the same certificate for SSL among all of the MLR services.

In addition to its own SSL certs, this service must also be able to connect to a running Water Auth server locally, and thus must trust the SSL certificate being served by Water Auth. This can be accomplished by copy-pasting the .crt file that Water Auth is serving into the `docker/certificates/import_certs` folder of this project. Any .crt file put into the `import_certs` directory will be loaded into the certificate store used by Python within the container and trusted by the application.

When using MLR Local Dev this means copying the certificates (*.crt currently named wildcard.crt) that MLR Local Dev generates into 2 places in this project:

1. `docker/certificates` to be used as the SSL certs served by this service

2. `docker/certificates/import_certs` to have this service trust other services serving the MLR Local Dev SSL certs

and adding the Key files (a file ending in .key currently named wildcard.key) to:

1.  `docker/certificates`

### Building Changes

To build and run the application after completing the above steps you can run: `docker-compose up --build {profile}`, replacing `{profile}` with one of the options listed above.

The swagger documentation can then be accessed at <https://localhost:6026/swagger-ui.html>
