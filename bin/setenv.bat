::
:: Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html
:: _________        _____ __________________        _____
:: __  ____/___________(_)______  /__  ____/______ ____(_)_______
:: _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
:: / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
:: \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
::
:: Version: 3.6.0c.09012012
::

::
:: Exports GRIDGAIN_LIBS variable containing classpath for GridGain.
:: Expects GRIDGAIN_HOME to be set.
:: Can be used like:
::      call %GRIDGAIN_HOME%\bin\setenv.bat
:: in other scripts to set classpath using exported GRIDGAIN_LIBS variable.
::

@echo off

:: USER_LIBS variable can optionally contain user's JARs/libs.
:: set USER_LIBS=

::
:: Check GRIDGAIN_HOME.
::
if not "%GRIDGAIN_HOME%" == "" goto run
    echo %0, ERROR: GRIDGAIN_HOME environment variable is not found.
goto error_finish

:run
:: The following libraries are required for GridGain.
set GRIDGAIN_LIBS=%USER_LIBS%;%GRIDGAIN_HOME%\libs\activation-1.1.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\annotations-9.0.4.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\antlr-2.7.7.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\aopalliance-1.0.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\asm-3.3.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\aspectjrt-1.6.12.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\aspectjweaver-1.6.12.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\aws-java-sdk-1.2.15.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\bcel-5.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\cglib-nodep-2.2.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-beanutils-1.8.3.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-cli-1.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-codec-1.6.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-collections-3.2.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-jexl-2.1.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-lang-2.6.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-logging-1.1.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\commons-net-3.0.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\concurrent-1.3.4.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\cron4j-2.2.5.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\dom4j-1.6.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\edtftpj-1.5.6.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\ezmorph-1.0.6.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\grizzly-utils-1.9.43.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\groovy-1.8.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\groovypp-0.9.0_1.8.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\h2-1.3.162.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\hibernate-3.2.7.ga.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\hibernate-jpa-2.0-api-1.0.0.Final.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\httpclient-4.1.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\httpclient-cache-4.1.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\httpcore-4.1.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\httpmime-4.1.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jackson-core-asl-1.4.3.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\javassist-3.12.1.GA.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jboss-serialization-4.2.2.GA.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-continuation-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-http-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-io-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-server-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-util-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jetty-xml-7.2.2.v20101205.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jline-2.9.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\joda-time-2.0.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jsch-0.1.45.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\json-lib-2.4-jdk15.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jta-1.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\jtidy-r938.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\junit-4.10.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\log4j-1.2.16.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\lucene-core-3.5.0.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\mail-1.4.4.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\oro-2.0.8.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\scala-compiler-2.9.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\scala-library-2.9.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\scalaz-core_2.9.1-6.0.3.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\servlet-api-2.5.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\slf4j-api-1.6.4.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\slf4j-log4j12-1.6.4.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-aop-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-asm-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-beans-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-context-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-core-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-expression-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\spring-tx-3.1.0.RELEASE.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\stax-1.2.0.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\stax-api-1.0.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\trove-1.0.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\xmlpull-1.1.3.1.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\xpp3_min-1.1.4c.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\xstream-1.4.2.jar
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\config\userversion

:: Comment these jars if you do not wish to use Hyperic SIGAR licensed under GPL
:: Note that starting with GridGain 3.0 - Community Edition is licensed under GPLv3.
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\libs\sigar.jar

:: Uncomment if using JBoss.
:: JBOSS_HOME must point to JBoss installation folder.
:: set JBOSS_HOME=

:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\lib\jboss-common.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\lib\jboss-jmx.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\lib\jboss-system.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jbossha.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jboss-j2ee.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jboss.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jboss-transaction.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jmx-adaptor-plugin.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\server\all\lib\jnpserver.jar

:: If using JBoss AOP following libraries need to be downloaded separately
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\lib\jboss-aop-jdk50.jar
:: set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%JBOSS_HOME%\lib\jboss-aspect-library-jdk50.jar

:: Set user external libraries
call :setallext "%GRIDGAIN_HOME%\libs\ext" %*
goto error_finish

:setallext
if .%1.==.. goto error_finish
set dir=%1
set dir=%dir:"=%
if not "%GRIDGAIN_LIBS%"=="" set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%dir%
if "%GRIDGAIN_LIBS%"=="" set GRIDGAIN_LIBS=%dir%
for %%i in ("%dir%\*.jar") do call :setoneext "%%i"
for %%i in ("%dir%\*.zip") do call :setoneext "%%i"
shift
goto setallext

:setoneext
set file=%1
set file=%file:"=%
set GRIDGAIN_LIBS=%GRIDGAIN_LIBS%;%file%

:error_finish
