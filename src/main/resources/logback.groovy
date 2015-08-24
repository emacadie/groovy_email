//
// Built on Thu Jun 11 10:36:44 CEST 2015 by logback-translator
// For more information on configuration files in Groovy
// please see http://logback.qos.ch/manual/groovy.html

// For assistance related to this tool or configuration files
// in general, please contact the logback user mailing list at
//    http://qos.ch/mailman/listinfo/logback-user

// For professional support please see http://www.qos.ch/shop/products/professionalSupport

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

import static ch.qos.logback.classic.Level.DEBUG

appender( "STDOUT", ConsoleAppender ) {
  encoder( PatternLayoutEncoder ) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}
appender( "FILE-smtp", RollingFileAppender ) {
  rollingPolicy( TimeBasedRollingPolicy ) {
    fileNamePattern = "log/smtp.%d{yyyy-MM-dd}.log"
    maxHistory = 7
  }
  encoder( PatternLayoutEncoder ) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}
root( DEBUG, [ "STDOUT" ] )
logger( "info.shelfunit.smtp", INFO, [ "FILE-smtp" ] )
////////////////////////////////////
/*
appender("CONSOLE-postoffice", ConsoleAppender) {
  target = "System.out"
  encoder(PatternLayoutEncoder) {
    pattern = "%d %-5level [%thread] %logger{0}: %msg%n"
  }
}
*/
appender( "FILE-postoffice", RollingFileAppender ) {
  rollingPolicy( TimeBasedRollingPolicy ) {
    fileNamePattern = "log/post.office.%d{yyyy-MM-dd}.log"
    maxHistory = 7
  }
  encoder( PatternLayoutEncoder ) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}
// root( DEBUG, [ "CONSOLE-stdout" ] )
logger( "info.shelfunit.postoffice", INFO, [ "FILE-postoffice" ] )

