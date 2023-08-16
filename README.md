zocalo
=======

This is a branch of the original Zocalo project, created by Chris Hibbert from 2007-2011.

Three main capabilities were added as part of the branch:

1. Ability for participants to chat within an experiment and develop reputation scores.
2. Ability to precede the experiment with web-based instructions.
3. Improved after-experiment log parsing for reporting purposes.

# Building the Code

This code is steady state and further builds are not supported. If the need arises, `Maven 3.6.3` was used to generate the 1.2 release WAR of the code using the pom.xml.

Alternatively, the code can be built with `Grade 2.5` and `Groovy 2.3.10`. In either case, the software leverages `Java 1.8`.

# Installing the Code

Zocalo runs on a Java application server and has been tested with Apache Tomcat 8. To install the code:

1. Download the 1.2 WAR from https://github.com/jjdm/zocalo/releases/tag/1.2.
2. Download Tomcat 8 from https://tomcat.apache.org/download-80.cgi.
3. Install Tomcat per the instructions.
4. In Tomcat's webapps folder, rename the ROOT folder to something else - e.g. tomcat.
5. Copy the downloaded WAR into the Tomcat webapps folder.
6. Rename the WAR from `zocalo-1.2.war` to `ROOT.war`
7. Start Tomcat normally.
8. Browse to the URL (http://localhost:8080/Experimenter.jsp) locally, replacing `localhost` with the server name when appropriate.

The log files for each experiment will be inside of Tomcat's logs directory.
