groovy_email
============

An email server written in Groovy   

Currently, it can only receive, but not send.  

Attempts to implement SSL have not been succesful.   

I might just do SMTP AUTH PLAIN and leave it at that for now.    

People have to log into a POP server to use the SMTP, so I might use that as well.   

It seems like TLS is pretty hard.   

Also: I am sticking with PostGres, and testing against the DB.   

Uses dnsjava: http://www.dnsjava.org/  

