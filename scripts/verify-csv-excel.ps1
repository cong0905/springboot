param([string]$Classes='target-h2/classes')
$ErrorActionPreference='Stop'
$repairRoot=Split-Path $PSScriptRoot -Parent
$repairFixture=Join-Path $repairRoot '.local/csv-fixture.jsh'
$repairCsv=Join-Path $repairRoot '.local/csv-safe.csv'
$repairXlsx=Join-Path $repairRoot '.local/csv-safe-roundtrip.xlsx'
$repairJava=@'
import vn.techshop.reporting.ReportExportService;
import java.nio.file.*;
import java.nio.charset.*;
String[] csvPayloads={"=1+1","+1+1","-5","@SUM(1,1)","\t=1+1","\r=1+1","  =1+1","Nguyễn Văn A","Hà Nội, Việt Nam","Cửa hàng \"TechShop\"","Dòng 1\nDòng 2","0901234567"};
String csvEvidence="\uFEFFGiá trị\n";
for(int i=0;i<csvPayloads.length;i++)csvEvidence+=(i==csvPayloads.length-1?ReportExportService.escapeCsvText(csvPayloads[i]):ReportExportService.escapeCsv(csvPayloads[i]))+"\n";
Files.writeString(Path.of(".local/csv-safe.csv"),csvEvidence,StandardCharsets.UTF_8);
/exit
'@
Set-Content -LiteralPath $repairFixture -Value $repairJava -Encoding utf8
Push-Location $repairRoot
try {
 & jshell '--class-path' $Classes $repairFixture *> .local/csv-fixture.log
 if($LASTEXITCODE -ne 0 -or (Get-Content .local/csv-fixture.log -Raw) -match 'Error:'){throw 'Java CSV fixture generation failed.'}
 $repairExcel=New-Object -ComObject Excel.Application
 $repairExcel.Visible=$false
 $repairExcel.DisplayAlerts=$false
 $repairExcel.AutomationSecurity=3
 $repairBook=$null
 try {
  $repairBook=$repairExcel.Workbooks.Open($repairCsv)
  function Read-RepairCells($Book) {
   $repairRows=@()
   for($repairRow=2;$repairRow -le 13;$repairRow++) {
    $repairCell=$Book.Worksheets.Item(1).Cells.Item($repairRow,1)
    $repairRows+=@{row=$repairRow;value=$repairCell.Value2;hasFormula=[bool]$repairCell.HasFormula}
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($repairCell)
   }
   return $repairRows
  }
  $repairBefore=@(Read-RepairCells $repairBook)
  $repairBook.SaveAs($repairXlsx,51)
  $repairBook.Close($false)
  [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($repairBook)
  $repairBook=$repairExcel.Workbooks.Open($repairXlsx)
  $repairAfter=@(Read-RepairCells $repairBook)
  if(@($repairBefore+$repairAfter | Where-Object hasFormula).Count){throw 'Excel evaluated a CSV value as a formula.'}
  foreach($repairRows in @($repairBefore,$repairAfter)) {
   if($repairRows[11].value -ne "'0901234567"){throw 'Phone leading zero was not preserved as guarded text.'}
   if($repairRows[7].value -ne 'Nguyễn Văn A' -or $repairRows[8].value -ne 'Hà Nội, Việt Nam' -or $repairRows[9].value -ne 'Cửa hàng "TechShop"' -or $repairRows[10].value -ne "Dòng 1`nDòng 2"){throw 'Unicode or CSV escaping changed after Excel round trip.'}
  }
  @{result='PASS';capturedAt=(Get-Date).ToString('o');excelVersion=$repairExcel.Version;method='Compiled Java exporter; actual Excel default CSV open, save XLSX, close and reopen';note='Phone is explicitly guarded text. Excel retains the leading apostrophe; no formula is used.';before=$repairBefore;after=$repairAfter} | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $repairRoot 'qa/repair-continuation/csv-excel.json')
  Write-Output 'PASS: 12 values, no formulas before/after XLSX save, Unicode and phone leading zero preserved.'
 } finally {
  if($repairBook){$repairBook.Close($false);[void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($repairBook)}
  $repairExcel.Quit()
  [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($repairExcel)
 }
} finally {Pop-Location}
