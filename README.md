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

1. Download Perl from https://www.perl.org/get.html.
2. Install Perl per the instructions.
3. Download the 1.2 WAR from https://github.com/ryankfrench/experimental_asset_market_with_communication/releases/tag/Final.
4. Download Tomcat 8 from https://tomcat.apache.org/download-80.cgi.
5. Install Tomcat per the instructions.
6. In Tomcat's webapps folder, rename the ROOT folder to something else - e.g. tomcat.
7. Copy the downloaded WAR into the Tomcat webapps folder.
8. Rename the WAR from `zocalo-1.2.war` to `ROOT.war`
9. Start Tomcat normally.
10. Browse to the URL (http://localhost:8080/Experimenter.jsp) locally, replacing `localhost` with the server name when appropriate.
11. From the Experimenter page, upload a configuration file. From the config directory in the source, use CONFIGURATION or Chat.txt.  CONFIGURATION has detailed comments with more context but Chat.txt was used for experiment sessions.
11. Clients can connect two ways: at (http://localhost:8080/Login.jsp) and logging in with a trader name or at (http://localhost:8080/Trader.jsp?userName=trader_name) replacing `localhost` with the server name and `trader_name` with any trader name defined in the configuration file.

The log files for each experiment will be inside of Tomcat's logs directory. They can be downloaded from the Experimenter page after the conclusion of the experiment. If Perl was installed as described in step 2. a .csv file will also be available for download.
