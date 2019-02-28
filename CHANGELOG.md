# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html). (Patch version X.Y.0 is implied if not specified.)

## [0.4.5] - 2019-03-01
### Added
- user-friendly report output to email body.
- Spring AOP configuration for profiling transactions when logging level is set to TRACE for org.springframework.aop and gov.usgs.wma.mlrgateway
- error handling for a 403 (unauthorized) http error.

## Changed
- Standardized feignclient method response types. 
- updated date parsing to display in browser's time zone. 

## [0.4.4] - 2019-01-31
### Added
- report success boolean in site reports.
- throw error when location is not found for an update
- Unparsed json in download step report link 

### Changed
- Catch and report errors from Ddot Ingester service
- Disabled TLS 1.0/1.1 by default.
- standardize error messages
- user-friendly report output to web user interface.
- Modified email attachment file names to be more specific

## [0.4.3] - 2018-11-27
## Added
- usage of the CruService's duplicate validation in the LegacyValidatorService.
- Help tooltip to Export Workflow UI. 
- new models to store various site and top-level workflow information.
 
### Changed
- Moved transformation before validation in workflow. 
- Split one transform call into a pre-validation and post-validation call.
- return logging statement with http response changed to 200 if no site is found on station create.
- Switched Export Workflow UI to use "Copy" language. 

### Removed
- 404 error status message on a successful add transaction

## [0.4.2] - 2018-10-18
### Changed
- Support file uploads larger than the default 1MB and better handling of large JSON reports in the UI.
- Attempting an empty export now properly displays the error.

## [0.4.1] - 2018-09-27
### Added
- Ability to configure the session timeout time and increased the default to 12 hours from 30 mins.
- Configure spring autoconfigure session timeout for JdbcSession. 

### Changed
- Fixed a bug preventing the UI from handling error responses not containing a JSON map. 
- Fixed ordering of if logic for handling errors in the UI. 

### Removed
- EnableJdbcHttpSession annotation so that spring boot configuration would win.

## [0.4.0] - 2018-08-23
### Added
- Dockerfile Healthcheck
- additional environment, "test"

### Changed
- Allow anonymous users on pages that have '.permitAll()' 

### Removed
- Dockerfile
- Dockerfile-DOI
- Reference to Docker usage in Readme


## [0.3.0] - 2017-11-20
### Added
- Global exception handler for Http requests
- Security - User must be authenticated to access any service. User must also be authorized for dDot workflow.
- HTTPS Support.
- Multiple transaction dDot file support.
- User interface migrated to this project.

### Changed
- Support for multiple notification recipients.
- Better handling of errors from backing services.

## [0.2.1] - 2017-11-03
### Changed
- New validation API.

## [0.2] - 2017-10-18
### Added
- Expanded dDot workflow to include all backing services.
- Export workflow.
- Notification Service enhancements (JSON).
- Configurable notification email.

## 0.1 - 2017-10-02
### Added
- Initial release - happy path - dDot injest and CRU only.

[Unreleased]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.3.0...master
[0.4.5]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.4.4...mlrgateway-0.4.5
[0.4.4]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.4.3...mlrgateway-0.4.4
[0.4.3]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.4.2...mlrgateway-0.4.3
[0.4.2]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.4.1...mlrgateway-0.4.2
[0.4.1]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.4.0...mlrgateway-0.4.1
[0.4.0]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.3.0...mlrgateway-0.4.0
[0.3.0]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.2.1...mlrgateway-0.3.0
[0.2.1]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.2...mlrgateway-0.2.1
[0.2]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.1...mlrgateway-0.2
