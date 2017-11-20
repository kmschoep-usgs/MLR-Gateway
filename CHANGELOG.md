# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html). (Patch version X.Y.0 is implied if not specified.)

## [Unreleased]

## [0.3.0] - 2017-11-20
###Added
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
[0.3.0]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.2.1...mlrgateway-0.3.0
[0.2.1]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.2...mlrgateway-0.2.1
[0.2]: https://github.com/USGS-CIDA/MLR-Gateway/compare/mlrgateway-0.1...mlrgateway-0.2
