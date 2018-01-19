#!/bin/bash
# update-rc.d groovy.mail.server.sh start 50 0 1 2 3 4 5 6 . stop 50 0 1 2 3 4 5 6 .
# description: groovy mail Server
# chkconfig: 2345 99 00

# export PATH=/usr/lib/jvm/java-7-openjdk-i386/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games
export PATH=/usr/local/java/jdk1.8.0_40/bin/java:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games
export JAVA_HOME=/usr/local/java/jdk1.8.0_40/
CLASSPATH=.:$JAVA_HOME/jre/lib/rt.jar

case "$1" in
	'start')
		/home/ericm/groovy_email/bin/MailRunner.sh /home/ericm/application.conf &

		;;
	'stop')
		for i in `pgrep -f .*java.*groovy_email.*`;  do     kill -9 $i;  done

		;;
	'restart')
		for i in `pgrep -f .*java.*groovy_email.*`;  do     kill -9 $i;  done

		sleep 60
		/home/ericm/groovy_email/bin/MailRunner.sh /home/ericm/application.conf & 

		;;
	*)
		echo "Usage: $0 { start | stop | restart }"
		;;
esac
exit 0

