# cadc-vodml 

Basic VO-DML/XML validator. VO-DML/XML is a document format to describe data models and includes
both <a href="https://www.w3.org/XML/Schema">XML Schema</a> and <a href="http://schematron.com/">ISO Schematron</a>
validation. This module implements validation of such a data model description using both the VO-DML schema and
schematron constraints.

Future goals:
- the VOModelReader currently validates VO-DML/XML documents but just returns a JDOM Document; the intent is to
implement or re-use a basic set of domain classes ansd return, for example, an instance of DataModel.

The VOModelReader can be easily used in unit tests to validate a VO-DML document before running further processes 
such as generating UML diagrams or HTML documentation. 

The included command-line wrapper is pretty basic but functional.

Note: one of the library dependencies (ph-schematron) requires Java 8.

