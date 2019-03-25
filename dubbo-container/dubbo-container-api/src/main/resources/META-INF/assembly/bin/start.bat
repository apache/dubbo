@echo off & setlocal enabledelayedexpansion

set LIB_JARS=""
set VM_ARGS_PERM_SIZE="MaxPermSize"
set VM_ARGS_METASPACE_SIZE="MaxMetaspaceSize"
set JAVA_8_VERSION="180"
cd ..\lib
for %%i in (*) do set LIB_JARS=!LIB_JARS!;..\lib\%%i
cd ..\bin

@REM set jvm args by different java version
for /f tokens^=2-4^ delims^=.-_+^" %%j in ('java -fullversion 2^>^&1') do set "JAVA_VERSION=%%j%%k%%l"
set VM_ARGS=%VM_ARGS_PERM_SIZE%
if "%JAVA_VERSION%" GEQ %JAVA_8_VERSION% set VM_ARGS=%VM_ARGS_METASPACE_SIZE%
if ""%1"" == ""debug"" goto debug
if ""%1"" == ""jmx"" goto jmx

java -Xms64m -Xmx1024m -XX:%VM_ARGS%=64M -classpath ..\conf;%LIB_JARS% org.apache.dubbo.container.Main
goto end

:debug
java -Xms64m -Xmx1024m -XX:%VM_ARGS%=64M -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -classpath ..\conf;%LIB_JARS% org.apache.dubbo.container.Main
goto end

:jmx
java -Xms64m -Xmx1024m -XX:%VM_ARGS%=64M -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -classpath ..\conf;%LIB_JARS% org.apache.dubbo.container.Main

:end
pause