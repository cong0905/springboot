param([int]$Port=8080)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$jar=Join-Path $projectRoot 'target\springboot-0.1.0-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Run .\mvnw.cmd verify first.'}
$jdk=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\java.exe'}else{'C:\Program Files\Java\jdk-21\bin\java.exe'}
& $jdk '-jar' $jar '--spring.profiles.active=demo' "--server.port=$Port"
