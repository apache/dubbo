@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.  See the NOTICE file distributed with
@REM this work for additional information regarding copyright ownership.
@REM The ASF licenses this file to You under the Apache License, Version 2.0
@REM (the "License"); you may not use this file except in compliance with
@REM the License.  You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------

@echo off
setlocal enabledelayedexpansion

set MAVEN_OPTS=^
-Xms2g ^
-Xmx2g ^
-XX:+UseG1GC ^
-XX:InitiatingHeapOccupancyPercent=45 ^
-XX:+UseStringDeduplication ^
-XX:-TieredCompilation ^
-XX:TieredStopAtLevel=1 ^
-Dmaven.build.cache.enabled=true ^
-Dmaven.build.cache.lazyRestore=true ^
-Dmaven.compiler.useIncrementalCompilation=false ^
-Dcheckstyle.skip=true ^
-Dcheckstyle_unix.skip=true ^
-Drat.skip=true ^
-Dmaven.javadoc.skip=true

set CMD=mvnw.cmd -e --batch-mode --no-snapshot-updates --fail-fast -T 2C
set ARGS=
set MODULES=
set PROFILES=sources,skip-spotless
set DEFAULT_MODULES=dubbo-distribution/dubbo-all,dubbo-spring-boot/dubbo-spring-boot-starter
set TEST_SKIP=true

goto parse_args

:print_help
echo Usage: %~n0 [options]
echo Fast local compilation with incremental build and caching
echo Options:
echo   -c    Execute clean goal (removes build artifacts)
echo   -p    Execute compile goal (compiles the source code)
echo   -i    Execute install goal (builds and installs the project)
echo   -t    Execute test goal (runs the tests)
echo   -s    Execute spotless:apply (format the code)
echo   -d    Execute dependency:tree (displays the dependency tree)
echo   -m    Specify modules, default is %DEFAULT_MODULES%
echo   -f    Specify profiles, default is %PROFILES%
echo   -h    Display this help message
echo.
echo Examples:
echo   %~n0                          Execute install goal compilation
echo   %~n0 -m                       Execute a minimal compilation
echo   %~n0 -c -i                    Execute clean, install goals compilation
echo   %~n0 -s                       Execute spotless:apply
echo   %~n0 -d                       Display the dependency tree
echo   %~n0 -t -m dubbo-config       Execute test goal for dubbo-config module
echo   %~n0 -c -p -m dubbo-common    Execute clean, compile the dubbo-common module
exit /b

:parse_args
set ARG=%~1
if "%ARG%"=="" goto check_args
if "%ARG%"=="-c" (
    set ARGS=%ARGS% clean
) else if "%ARG%"=="-p" (
    set ARGS=%ARGS% compile
) else if "%ARG%"=="-i" (
    set ARGS=%ARGS% install
) else if "%ARG%"=="-t" (
    set ARGS=%ARGS% test
    set TEST_SKIP=false
) else if "%ARG%"=="-s" (
    set ARGS=%ARGS% spotless:apply
    set PROFILES=sources
) else if "%ARG%"=="-d" (
    set ARGS=%ARGS% dependency:tree
) else if "%ARG%"=="-m" (
    if "%~2"=="" (
        set MODULES= -pl %DEFAULT_MODULES% -am
    ) else (
        set MODULES= -pl %~2 -am
        shift
    )
) else if "%ARG%"=="-f" (
    set PROFILES=%~2
    shift
) else if "%ARG%"=="-h" (
    goto print_help
) else (
    set ARGS=%ARGS% %ARG%
)

shift
goto parse_args

:check_args
if "%TEST_SKIP%"=="true" (
    set MAVEN_OPTS=%MAVEN_OPTS% -Dmaven.test.skip=true
)
if "%ARGS%"=="" (
    set ARGS= install
)

@echo on
%CMD%%ARGS%%MODULES% -P %PROFILES%

endlocal
