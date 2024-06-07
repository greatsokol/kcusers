# KCUsers (Keycloak users)

Service that queries the keycloak API to obtain the last authentication time of keyloak users.

The frequency of surveys is determined using cron `service.cron`.

Users who were not authenticated last
`service.keycloakclient.inactivity.days` days, are blocked in keyloak using keyloak API.

Users who have been unblocked in this service must authenticate in `service.keycloakclient.inactivity.immunityperiodminutes` minutes.

Otherwise, they will be blocked again.

A web application for monitoring the service status is available on `localhost:9000`.

Users who can be monitored are determined by roles `front.userroles`.

Users who can make changes are defined by roles `front.adminroles`.

Observable realms are `service.keycloakclient.realms`.

Passwords are in enviroment variables `kсpwd`, `pgpwd`, `clsec`.