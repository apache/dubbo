@echo off
set username=root
set password=mysql
set devuser=dubbo_registry
set devuserpass=dubbo_registry
set database=dubbo_registry
set sqlpath=..\..\..\..\..\dubbo.registry.server\src\main\assembly\sql\mysql.sql

echo.
echo prerequisites check...

mysql --version 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo --mysql client exist, okay!) ELSE (GOTO NO_MYSQL_CLIENT)

:: check if we can login to database with root account
ECHO select 1 | mysql -u %username% -p%password% 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo --mysql login with root, okay!) ELSE (GOTO CANT_LOGIN)

:: check if populate sql exists
IF EXIST %sqlpath% (echo --mysql.sql exist, okay!) ELSE GOTO SQLPATH_INVALID

echo prerequisites all okay!
echo.


:: check if dubbo_registry exists
ECHO use %database% | mysql -u %username% -p%password% 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo %database% exists, drop it.) ELSE (GOTO NO_DROP)

:: drop database
ECHO drop database %database% | mysql -u %username% -p%password% 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo %database% dropped.) ELSE (GOTO DROP_FAILED)

:NO_DROP
:: re-create the database
ECHO create database %database% | mysql -u %username% -p%password% 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo %database% re-created.) ELSE (GOTO CREATE_FAILED)

:: create user and grant priviliges 
ECHO grant all privileges on %database%.* to %devuser%@'%' identified by '%devuserpass%' | mysql -u %username% -p%password% 1> nul 2>nul
IF %ERRORLEVEL% == 0 (echo privilige of %database% to %devuser% granted.) ELSE (GOTO GRANT_FAILED)

:: populate tables
mysql -u %username% -p%password% %database% < %sqlpath% 
IF %ERRORLEVEL% == 0 (echo tables for %database% populated.) ELSE (GOTO POPULATE_FAILED)

GOTO SUCCESS

:: error handling section
:NO_MYSQL_CLIENT
echo please install mysql client first!
goto FAILED

:CANT_LOGIN
echo please ensure: 1. mysql service is on. 2. root password is 111111
goto FAILED

:SQLPATH_INVALID
echo sql file path (%sqlpath%) does not exist!
goto FAILED

:DROP_FAILED
echo drop database %database% failed!
goto FAILED

:DROP_FAILED
echo re-create database %database% failed!
goto FAILED

:POPULATE_FAILED
echo populate tables for database %database% failed!
goto FAILED

:GRANT_FAILED
echo privilige of %database% to %username% grant failed!
goto FAILED

:SUCCESS
echo.
echo dev environment setup successfully!

:FAILED

