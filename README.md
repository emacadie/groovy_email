groovy_email
============

An email server written in Groovy   

Currently, it can only receive, but not send.  

Attempts to implement SSL have not been succesful.   

I might just do SMTP AUTH PLAIN and leave it at that for now.    

People have to log into a POP server to use the SMTP, so I might use that as well.   

It seems like TLS is pretty hard.   

Also: I am sticking with Postgres, and testing against a test Postgres DB.   
See YCombinator story [Don't test with SQLite when you use Postgres in Production](https://news.ycombinator.com/item?id=10002142)    

Uses dnsjava: http://www.dnsjava.org/  

Uses clamav: On Ubuntu: apt-get install clamav-daemon    
https://help.ubuntu.com/community/ClamAV    
Setting up clamav-daemon (0.98.7+dfsg-0ubuntu0.14.04.1) ...
 * Clamav signatures not found in /var/lib/clamav
 * Please retrieve them using freshclam
 * Then run '/etc/init.d/clamav-daemon start'   
There are a few tests that use ClamAV. To run them, do this: gradle test -Dclam.live.daemon=true  
To run tests without ClamAV (but with Mocks), do this: gradle test -Dclam.live.daemon=false  

To configure the app, look at src/test/resources/application.test.conf   
To run it, do gradle distZip, and get build/distributions/groovy_email.zip  
Unzip it  
cd to groovy_email/bin    
Run ./MailRunner.sh /path/to/application.conf &    
There is a script for adding users:  
```
./UserInserter.sh -configPath /path/to/application.conf -user uname -fName John -lName Doe -pass N5ecure -iterations 10   
```



