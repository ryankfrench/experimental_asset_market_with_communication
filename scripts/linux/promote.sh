BASE_1_BIN=/opt/tomcat8/base1/bin/
BASE_2_BIN=/opt/tomcat8/base2/bin/
CHAPMAN_HOME=~/git/chapman/
CHAPMAN_BRANCH=instructions_loan
#CHAPMAN_BRANCH=chat_01
ZOCALO_HOME=~/git/zocalo/
ZOCALO_BRANCH=master
#ZOCALO_BRANCH=participation

printf "\nShutting down Tomcat.\n"
cd $BASE_1_BIN
./shutdown.sh
cd $BASE_2_BIN
./shutdown.sh

printf "\nPulling latest Instructions from GitHub and building Tomcat.\n"
cd $CHAPMAN_HOME
git checkout $CHAPMAN_BRANCH
git pull origin $CHAPMAN_BRANCH
git log -n 1
gradle tomcat

printf "\nPulling latest Zocalo from GitHub and building Tomcat.\n"
cd $ZOCALO_HOME
git checkout $ZOCALO_BRANCH
git pull origin $ZOCALO_BRANCH
git pull
git log -n 1
gradle tomcat

printf "\nStarting up Tomcat.\n"
cd $BASE_1_BIN
nohup ./startup.sh &
cd $BASE_2_BIN
nohup ./startup.sh &

printf "Both Base 1 and Base 2 Started and Instructions / Zocalo copied."
