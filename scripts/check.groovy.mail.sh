#!/bin/bash

LOGGER="/usr/bin/logger"
export JAVA_HOME=/usr/local/java/jdk1.8.0_40/
export PATH=/usr/local/java/jdk1.8.0_40/bin/java:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games
CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar
JAMES_HOME=/usr/local/java/james-2.3.2/

#- A function that will log what we do
log() {
	echo "$1"
	test -x "$LOGGER" && $LOGGER -p info "$1"
}


log 'seeing if we need to restart groovy_email'


total=0
for i in `pgrep -f .*java.*groovy_email.*`
do
	(( total =  total + 1 ))
done
echo $total
if [ $total = 0 ]; then
	echo "Total is 0 for groovy_email"
	log 'total is 0, calling restart groovy_email'
	/etc/init.d/groovy.mail.server.sh restart
	log 'just called restart groovy_email'
else
	echo "Total is more than 0 for groovy_email"
	log 'no need to call restart groovy_email'

fi
