@echo off

set DIRNAME=%~dp0
IF "%JAVA_HOME%"=="" GOTO nojavahome
set JAVA="%JAVA_HOME%\bin\java"

for %%B in (%~dp0\.) do set KCUSERS_APP_HOME=%%~dpB
for /f %%i in ('dir ..\lib\kcusers-*.jar /s /b') do set runjar=%%i

set APP_CLASSPATH=%runjar%

set JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
REM The path to the heap dump location, note directory must exists and have enough
REM space for a full heap dump.
REM set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=$GRAVITEE_HOME/logs/heapdump.hprof

REM Disables explicit GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC

REM Ensure UTF-8 encoding by default (e.g. filenames)
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dapp.home=%KCUSERS_APP_HOME%
set JAVA_OPTS=%JAVA_OPTS% -Dlogging.config=%KCUSERS_APP_HOME%/config/logback.xml
set JAVA_OPTS=%JAVA_OPTS% -Dspring.config.location=%KCUSERS_APP_HOME%/config/application.yaml
SET clsec=CIs66oQuBCUh9aPPPk7sGnLLR4sVRb6b
SET pglpwd=admin
SET kсpwd=admin
REM Display our environment
echo "=============================================================="
echo ""
echo "  KCUSERS_APP_HOME: %KCUSERS_APP_HOME%"
echo ""
echo "  JAVA: %JAVA%"
echo ""
echo "  JAVA_OPTS: %JAVA_OPTS%"
echo ""
echo "  CLASSPATH: %APP_CLASSPATH%"
echo ""
echo "=============================================================="
echo ""

%JAVA% %JAVA_OPTS% -cp %APP_CLASSPATH% org.gs.kcusers.KCUsersApplication "%*"


goto endbatch


:nojavahome
echo.
echo **************************************************
echo *
echo * WARNING ...
echo * JAVA_HOME must be set before starting
echo * Please check Java documentation to do it
echo *
echo **************************************************
GOTO endbatch

:endbatch

pause
