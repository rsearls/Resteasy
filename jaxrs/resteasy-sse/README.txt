
-- SPECIAL setup requirements

The javax.ws.rs.sse* apis are only in a maven archive that is not publicly
available (javax.ws.rs:javax.ws.rs-api:2.1-SNAPSHOT).  To build this project
the user needs to first install and build javax.ws.rs-api locally.

 project code: git://java.net/jax-rs-spec~api


-- Running the test case
The test case is in module Resteasy/jaxrs/arquillian/RESTEASY-SSE-TEST-WF8/
The maven-surefire-plugin module can not resolve the reference to javax.ws.rs:javax.ws.rs-api:2.1-SNAPSHOT
because it does not reside in a remote repository (i.e. it does not ref your local repository).  A
copy of the jar has been placed in RESTEASY-SSE-TEST-WF8/lib and a config property set in maven-surefire-plugin
to reference it.  

To run only this test,
   cd Resteasy/jaxrs/arquillian/RESTEASY-SSE-TEST-WF8/
   mvn clean test













