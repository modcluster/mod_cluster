@echo off
setlocal ENABLEDELAYEDEXPANSION

set CP=

for %%i in (lib\*.jar) do call :concatsep %%i

set "OPTS=-Xmn200M -Xmx300M -Xms300M -Xss8K -XX:ThreadStackSize=8k -XX:CompileThreshold=100 -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31"

REM Tell the HttpURLConnection pool to maintain 400 connections max
set "OPTS=%OPTS% -Dhttp.maxConnections=400"

REM Set defaults for the load balancer
set "OPTS=%OPTS% -Dmod_cluster.proxy.host=localhost -Dmod_cluster.proxy.port=8000"

java -cp %CP% %OPTS% org.jboss.modcluster.demo.client.ModClusterDemo 

goto end

:concatsep

if "%CP%" == "" (
set CP=%1
)else (
set CP=%CP%;%1
)

:end
