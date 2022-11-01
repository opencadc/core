# cadc-log

The `cadc-log` library provides a single servlet to initialize logging using
the log4j framework. The idea is to include this servlet in the web.xml with
`load-on-startup` of 1 (first) and configure standard logging there.

It is highly recommended that the default log level in web.xml be at `info` level.

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
        ca.nrc.cadc.db
      </param-value>
    </init-param>
    <!-- optional hard coded group permission -->
    <init-param>
        <param-name>logAccessGroup</param-name>
        <param-value>ivo://cadc.nrc.ca/gms?CADC</param-value>
    </init-param>
    <init-param>
        <param-name>groupAuthorizer</param-name>
        <param-value>ca.nrc.cadc.ac.client.GroupAuthorizer</param-value>
    </init-param>
    <!-- optional runtime user and group permissions -->
    <init-param>
        <param-name>logControlProperties</param-name>
        <param-value>example-logControl.properties</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
```
The servlet will configure a default logging at `warn` level and the specified packages at
`info` level.

The LogControlServlet supports GET and POST requests to view and change the current log levels and/or
configured packages. This requires permission using either of the optional init params in the example
above. The latter runtime configuration of permissions is preferred because then configuration ends 
up in the config dir instead of hard coded inside the application (war file).

## example-logControl.properties

This file allows granting permission to use the LogControlServlet at runtime.
```properties
user = {X509 distinguished name}
user = {X509 distinguished name}

group = {IVOA GMS group identifier}
group = {IVOA GMS group identifier}
```
Both the `user` and `group` properties are optional and support multiple values. The simplest example 
used at CADC is:
```properties
group = ivo://cadc.nrc.ca/gms?CADC
```
which allows members of the CADC staff group to view and change log levels.
