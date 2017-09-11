# MLR-Gateway
Monitoring Location Gateway

## Running the Application
Copy the src/main/resources/application.yml file to you project root directory and change the substitution variables as needed.
Open a terminal window and navigate to the project's root directory.
Use the maven command ```mvn spring-boot:run``` to run the application.
It will be available at http://localhost:8080 in your browser.

ctrl-c will stop the application.

## Using Docker
To build the image you will need to provide the location of the jar within 
https://cida.usgs.gov/artifactory/mlr-maven/gov/usgs/wma/mlrgateway as follows:
``` 
% docker build --build-arg=0.1-SNAPSHOT/mlrgateway-0.1-SNAPSHOT.jar .
```

To run the image, you will need to provide as environment variables the substitution variables in the application.yml. The application
will be available on part 8080 within the container.
