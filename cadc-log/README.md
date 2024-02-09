# cadc-log

The `cadc-log` library provides a single servlet to initialize logging using the log4j framework. 

## REST API
Services may include `servlet-mapping` so that HTTP GET and POST requests can
be sent to the log control endpoint. All services in OpenCADC use `/logControl`
for consistency but this is not required. Permission to access the log control
endpoint are configured with a `cadc-log.properties` file at runtime.

## cadc-log.properties (optional)

This file can be added to service config to grant perrmission to use the LogControlServlet at runtime.
```properties
user = {X509 distinguished name}
user = {X509 distinguished name}

group = {IVOA GMS group identifier}
group = {IVOA GMS group identifier}
```
Both the `user` and `group` properties are optional and support multiple values. The specified
users are granted permission to view (GET) and change (POST) log levels in the running service.

## log control REST API

This is a very simple explanation; TODO: document with OpenAI so it can be included in service API docs.

view current log levels: `GET {base URL}/logControl`

change current log levels to debug: `POST level=DEBUG {base URL}/logControl`

change log levels back to info: `POST level=INFO {base URL}/logControl`

change log to debug for a specific package (prefix): `POST level=DEBUG&package=ca.nrc.cadc.auth {base URL}/logControl`

The `package` parameter can add new packages to the logging config that were not included by the service. These
packages become "tracked" and are subject to later log level changes that change the level for all packages. If 
the caller does not want that package to be tracked, they can include `notrack=1` to prevent and retain manual 
control. 

All changes to logging (level and tracked packages are lost if the service is restarted.

## developer usage
Developers include this servlet in the web.xml with `load-on-startup` of 1 (first) 
and configure standard logging there. It is highly recommended that the default log 
level in web.xml be at `info` level.

Example:
```xml
<servlet>
    <servlet-name>logControl</servlet-name>
    <servlet-class>ca.nrc.cadc.log.LogControlServlet</servlet-class>
    <init-param>
      <param-name>logLevel</param-name>
      <param-value>info</param-value>
    </init-param>
    <init-param>
      <param-name>logLevelPackages</param-name>
      <param-value>
        <!-- whitespace separated list of packages for INFO level -->
        ca.nrc.cadc.auth
        ca.nrc.cadc.net
        ca.nrc.cadc.vosi
      </param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<!-- optional servlet mapping to expose REST API -->
<servlet-mapping>
    <servlet-name>logControl</servlet-name>
    <url-pattern>/logControl</url-pattern>
</servlet-mapping>
```
The servlet will configure a default logging at `warn` level and the specified packages at
`info` level.


