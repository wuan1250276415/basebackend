@echo off
cd /d %~dp0
set JAVA_OPTS=-Xms512m -Xmx1024m -Dspring.profiles.active=dev
java %JAVA_OPTS% -cp "target/classes;target/lib/*" com.basebackend.user.UserApiApplication
