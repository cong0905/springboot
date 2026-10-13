param([string]$MySqlHome='C:\Program Files\MySQL\MySQL Server 8.0',[int]$Port=33317)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$dataRoot=Join-Path $projectRoot '.local\mysql-test'
$server=Join-Path $MySqlHome 'bin\mysqld.exe'
$client=Join-Path $MySqlHome 'bin\mysql.exe'
if (!(Test-Path -LiteralPath $server)) { throw "MySQL server not found: $server" }
if (!(Test-Path -LiteralPath $dataRoot)) {
 New-Item -ItemType Directory -Path $dataRoot -Force | Out-Null
 & $server '--no-defaults' "--basedir=$MySqlHome" "--datadir=$dataRoot" '--initialize-insecure' '--console' 2>&1 | Set-Content (Join-Path $projectRoot '.local\mysql-initialize.log')
 if($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed; see .local/mysql-initialize.log' }
 Set-Content (Join-Path $dataRoot '.techshop-test-owned') 'Disposable local TechShop test database. Never use customer data.'
}
if (!(Test-Path -LiteralPath (Join-Path $dataRoot '.techshop-test-owned'))) {throw 'Refusing to use a data directory not created by this script.'}
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {throw "Port $Port is already in use; choose an unused port."}
$arguments=@('--no-defaults',"--basedir=`"$MySqlHome`"","--datadir=`"$dataRoot`"",'--bind-address=127.0.0.1',"--port=$Port",'--mysqlx=OFF','--default-time-zone=+00:00','--console')
$mysqlProcess=Start-Process -FilePath $server -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $projectRoot '.local\mysql-test.out.log') -RedirectStandardError (Join-Path $projectRoot '.local\mysql-test.err.log')
Set-Content (Join-Path $projectRoot '.local\mysql-test.pid') $mysqlProcess.Id
for($attempt=0;$attempt -lt 30;$attempt++) {
 if(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {break}
 if($mysqlProcess.HasExited) {throw 'Test MySQL exited; see .local/mysql-test.err.log'}
 Start-Sleep -Seconds 1
}
& $client '--no-defaults' '--user=root' '--host=127.0.0.1' "--port=$Port" '-e' 'CREATE DATABASE IF NOT EXISTS techshop_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; SELECT VERSION();'
if($LASTEXITCODE -ne 0) {throw 'Cannot connect to isolated test MySQL'}
Write-Output "Test MySQL ready on 127.0.0.1:$Port (PID $($mysqlProcess.Id)); database techshop_test. Local disposable instance only."
