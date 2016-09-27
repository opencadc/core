# cadc-test package 

The cadc-rest package implements a very simple servlet-action pattern. It is intended to help 
implement simple web service endpoints by separating the service-specific code from the Servlet
API and handling common initialisation and error handling.

Applications that use this library can deploy the RestServlet and configure one or more 
subclasses of RestAction (0 or 1 per HTTP action). GET and POST are known to work; DELETE should 
work; PUT won't work yet because the body of the request is not exposed.
