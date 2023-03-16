This is the OpenCADC utility library. 

It includes classes that are very general purpose and used in multiple other OpenCADC
projects. 

## configuration

Some classes in the `cadc-util` library can have their behaviour controlled by
setting Java system properties.
```
# configure an IdentityManager implementation 
# (default: ca.nrc.cadc.auth.NoOpIdentityManager)
ca.nrc.cadc.auth.IdentityManager={class name of IdentityManager implementation}

# OBSOLETE: functionality merged into IdentityManager
ca.nrc.cadc.auth.Authenticator={class name of Authenticator implementation}

# capture a basic authorization attempt in an AuthorizationTokenPrincipal
# so it can be validated by the IdentityManager (default: false aka ignore)
ca.nrc.cadc.auth.PrincipalExtractor.allowBasicATP=true

# trust an external proxy doing SSL termination to pass a validated client
# certificate via the x-client-certificate header (default: false aka ignore)
ca.nrc.cadc.auth.PrincipalExtractor.enableClientCertHeader=true

#  configure logging to only print the message (INFO only?)
(default: false aka print extra log4j preamble)
ca.nrc.cadc.util.Log4jInit.messageOnly=true

# following to be verified:
ca.nrc.cadc.net.HttpTransfer.bufferSize={buffer size in bytes for ??}
ca.nrc.cadc.util.PropertiesReader.dir={alt config dir?}
```


* Note about the SSL support in ca.nrc.cadc.auth

The SSLUtil class is intended to support IVOA Single Sign On (SSO) so the
current functionaility is aimed at mutual authentication: both the client
and the server have X509 certificates. We assume the client is actually using 
a short-lived, no-password proxy certificate. The createProxyCert script uses
openssl to create a proxy certificate and associated priovate key for this 
purpose.

The SSLUtilTest code assumes that you have a valid proxy certificate named proxy.crt, 
associated private key (proxy.key) and a file that contains both the certificate and the
key (proxy.pem) in the classpath (e.g. in build/test/class). All three types of files 
can be created with the script:

./scripts/createProxyCert <your real cert> <your private key> <days> build/test/class/proxy

The test/resources/proxy.pem file can be used with curl, assuming your version of curl 
is built with SSL support.

The SSL tests try to connect to 3 different https URLS:
https://www.google.com/
https://<FQDN of localhost>/
https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/

Google has a valid server certificate and does not challenge for a client certificate.

We assume localhost has an invalid (eg self-signed) server certificate and the test is
intended to fail to trust the server. This is typical of a developer workstation, for
example. A second test with this server passes when a special system property is set 
(see BasicX509TrustManager); this is intended for use in test code only and not as a
work-around for using real services with invalid certificates!!

CADC's public web servers have valid server certificates and require client authentication via
certificate so they act as a decent SSO setup test.

Note: The local host test is currently disabled in the test code (pdowler 2010-07-15)

