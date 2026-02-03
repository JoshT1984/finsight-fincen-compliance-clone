@echo off
setlocal
set MAVEN_CMD=mvn
if not "%MAVEN_HOME%"=="" set MAVEN_CMD=%MAVEN_HOME%\bin\mvn

%MAVEN_CMD% %*
