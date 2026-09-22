# בוחר אפליקציות לבנייה. לחיצה כפולה על build-apps.bat פותחת חלון עם כל
# אפליקציות FutureOS; מה שמסמנים בו *לא* נבנה, וכל השאר נבנות אחת אחרי השנייה
# (assembleDebug, או installDebug אם מסמנים התקנה על המכשיר).
#
# גילוי האפליקציות זהה ל-build-all.sh: כל תיקייה ברמה העליונה שיש בה
# settings.gradle.kts היא פרויקט Gradle עצמאי, כך שאפליקציה חדשה מופיעה בחלון
# בלי לגעת בסקריפט. הבנייה סדרתית ולא מקבילית מאותה סיבה שמתועדת שם - נעילת
# קבצים על ה-build/ המשותף של SharedKeypadNav.
#
# החלון בעברית (WinForms מציג RTL כמו שצריך), אבל כל מה שנכתב לקונסול באנגלית:
# הטרמינל של Windows בלי תמיכת bidi ומציג עברית הפוכה.
#
# הקובץ שמור ב-UTF-8 עם BOM בכוונה: Windows PowerShell 5.1 קורא קובץ בלי BOM
# בקידוד ה-ANSI של המערכת, והעברית בחלון הייתה יוצאת ג'יבריש.
#
# קוד יציאה: 0 אם הכול הצליח או שהחלון בוטל, 1 אם משהו נכשל. build-apps.bat
# משאיר את הקונסול פתוח רק במקרה של 1, כדי שאפשר יהיה לקרוא את השגיאה.

$Root          = $PSScriptRoot
$SelectionFile = Join-Path $Root '.build-apps-selection.json'
$LogFile       = Join-Path $Root 'build-apps.log'
# מהניסיון בריפו הזה: בנייה של כל האפליקציות צריכה בערך 4GB פנויים, ודיסק
# מלא מתבטא בכשלונות Gradle מבלבלים באמצע. החלון רק מזהיר, לא חוסם.
$MinFreeGB     = 4

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# בלי זה Windows מגדיל את החלון כתמונה במסך עם scaling, והטקסט יוצא מטושטש
try {
    Add-Type -Namespace FutureOSBuild -Name Native -MemberDefinition '[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();'
    [void][FutureOSBuild.Native]::SetProcessDPIAware()
} catch { }
[System.Windows.Forms.Application]::EnableVisualStyles()

# מריץ פקודה ומשקף את הפלט שלה גם לקונסול וגם ללוג. חוזר כשהתהליך יוצא ולא
# כשה-pipe של הפלט נסגר: Gradle daemon (או Kotlin daemon / שרת adb) שעולה
# באמצע הבנייה יורש את ה-pipe ומחזיק אותו פתוח שעות, ו-"& cmd | ForEach-Object"
# של PowerShell היה מחכה ל-EOF הזה ונתקע אחרי האפליקציה הראשונה.
if (-not ('FutureOSBuild.Runner' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Diagnostics;
using System.IO;
using System.Threading;

namespace FutureOSBuild {
    public static class Runner {
        public static int Run(string workingDirectory, string commandLine, TextWriter log) {
            var info = new ProcessStartInfo("cmd.exe", "/d /c " + commandLine + " 2>&1");
            info.WorkingDirectory = workingDirectory;
            info.UseShellExecute = false;
            info.RedirectStandardOutput = true;

            var gate = new object();
            var done = false;
            var eof = false;
            var lastOutput = Environment.TickCount;
            var process = new Process();
            process.StartInfo = info;
            process.OutputDataReceived += (sender, e) => {
                lock (gate) {
                    if (done) return;
                    if (e.Data == null) { eof = true; return; }
                    lastOutput = Environment.TickCount;
                    Console.WriteLine(e.Data);
                    log.WriteLine(e.Data);
                }
            };
            process.Start();
            process.BeginOutputReadLine();
            // WaitForExit() without a timeout would also wait for EOF on the pipe
            while (!process.WaitForExit(1000)) { }

            // Lines written just before exit may still be in flight; drain them,
            // but stop waiting as soon as the output goes quiet.
            var deadline = Environment.TickCount + 5000;
            while (Environment.TickCount < deadline) {
                lock (gate) {
                    if (eof || Environment.TickCount - lastOutput > 500) break;
                }
                Thread.Sleep(50);
            }
            lock (gate) { done = true; }
            return process.ExitCode;
        }
    }
}
'@
}

function Get-Apps {
    Get-ChildItem -LiteralPath $Root -Directory |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'settings.gradle.kts') } |
        Sort-Object Name |
        ForEach-Object { $_.Name }
}

function Get-FreeGB {
    try {
        $drive = New-Object System.IO.DriveInfo([System.IO.Path]::GetPathRoot($Root))
        return [math]::Round($drive.AvailableFreeSpace / 1GB, 1)
    } catch {
        return -1
    }
}

# הבחירה האחרונה נשמרת כדי שלא יהיה צריך לסמן מחדש בכל פעם. קובץ חסר או
# פגום פשוט אומר להתחיל בלי סימונים - זה לא סיבה להיכשל.
function Read-Selection {
    $selection = @{ Excluded = @(); Install = $false }
    if (Test-Path -LiteralPath $SelectionFile) {
        try {
            $json = Get-Content -LiteralPath $SelectionFile -Raw -Encoding UTF8 | ConvertFrom-Json
            if ($json.PSObject.Properties['excluded']) {
                $selection.Excluded = @($json.excluded | Where-Object { $_ } | ForEach-Object { [string]$_ })
            }
            if ($json.PSObject.Properties['install']) {
                $selection.Install = [bool]$json.install
            }
        } catch { }
    }
    return $selection
}

function Save-Selection($Choice) {
    try {
        $json = ConvertTo-Json -InputObject @{ excluded = @($Choice.Excluded); install = [bool]$Choice.Install }
        [System.IO.File]::WriteAllText($SelectionFile, $json, (New-Object System.Text.UTF8Encoding($false)))
    } catch { }
}

function Show-Message {
    param(
        [string]$Text,
        [System.Windows.Forms.MessageBoxIcon]$Icon = [System.Windows.Forms.MessageBoxIcon]::Information,
        [System.Windows.Forms.MessageBoxButtons]$Buttons = [System.Windows.Forms.MessageBoxButtons]::OK
    )
    # חלון בעלים TopMost, כדי שהסיכום יקפוץ מעל הכול גם אחרי בנייה ארוכה ברקע
    $owner = New-Object System.Windows.Forms.Form
    $owner.TopMost = $true
    try {
        $options = [System.Windows.Forms.MessageBoxOptions]::RtlReading -bor [System.Windows.Forms.MessageBoxOptions]::RightAlign
        return [System.Windows.Forms.MessageBox]::Show($owner, $Text, 'FutureOS', $Buttons, $Icon,
            [System.Windows.Forms.MessageBoxDefaultButton]::Button1, $options)
    } finally {
        $owner.Dispose()
    }
}

# בונה את החלון בלי להציג אותו. ה-handlers הם closures (GetNewClosure) ולא
# נשענים על ה-scope של הפונקציה, כי הם רצים רק אחרי שהיא כבר חזרה.
function New-PickerForm {
    param([string[]]$Apps, [string[]]$Excluded, [bool]$Install, [double]$FreeGB)

    $font       = New-Object System.Drawing.Font('Segoe UI', 10)
    $strikeFont = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Strikeout)
    $boldFont   = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Bold)

    $form = New-Object System.Windows.Forms.Form
    $form.Text = 'FutureOS - בניית אפליקציות'
    $form.Font = $font
    $form.RightToLeft = [System.Windows.Forms.RightToLeft]::Yes
    $form.RightToLeftLayout = $true
    $form.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
    $form.MaximizeBox = $false
    $form.MinimizeBox = $false
    $form.StartPosition = [System.Windows.Forms.FormStartPosition]::CenterScreen
    $form.AutoSize = $true
    $form.AutoSizeMode = [System.Windows.Forms.AutoSizeMode]::GrowAndShrink
    $form.Padding = New-Object System.Windows.Forms.Padding(12)

    $layout = New-Object System.Windows.Forms.TableLayoutPanel
    $layout.AutoSize = $true
    $layout.AutoSizeMode = [System.Windows.Forms.AutoSizeMode]::GrowAndShrink
    $layout.ColumnCount = 1

    $title = New-Object System.Windows.Forms.Label
    $title.Text = 'סמנו את האפליקציות שלא ייבנו. כל השאר ייבנו אחת אחרי השנייה.'
    $title.Font = $boldFont
    $title.AutoSize = $true
    $title.Margin = New-Object System.Windows.Forms.Padding(3, 3, 3, 10)
    $layout.Controls.Add($title)

    # רשת של 3 עמודות, ממוינת לאורך כל עמודה (קל יותר למצוא שם)
    $columns = 3
    $rows = [int][math]::Ceiling($Apps.Count / $columns)
    $grid = New-Object System.Windows.Forms.TableLayoutPanel
    $grid.AutoSize = $true
    $grid.AutoSizeMode = [System.Windows.Forms.AutoSizeMode]::GrowAndShrink
    $grid.ColumnCount = $columns
    $grid.RowCount = $rows
    for ($c = 0; $c -lt $columns; $c++) {
        [void]$grid.ColumnStyles.Add((New-Object System.Windows.Forms.ColumnStyle([System.Windows.Forms.SizeType]::AutoSize)))
    }
    $boxes = New-Object 'System.Collections.Generic.List[System.Windows.Forms.CheckBox]'
    for ($i = 0; $i -lt $Apps.Count; $i++) {
        $box = New-Object System.Windows.Forms.CheckBox
        $box.Text = $Apps[$i]
        $box.AutoSize = $true
        $box.Margin = New-Object System.Windows.Forms.Padding(3, 2, 24, 2)
        $box.Checked = $Excluded -contains $Apps[$i]
        $grid.Controls.Add($box, [int][math]::Floor($i / $rows), $i % $rows)
        $boxes.Add($box)
    }
    $layout.Controls.Add($grid)

    $helpers = New-Object System.Windows.Forms.FlowLayoutPanel
    $helpers.AutoSize = $true
    $helpers.AutoSizeMode = [System.Windows.Forms.AutoSizeMode]::GrowAndShrink
    $helpers.WrapContents = $false
    $helpers.Margin = New-Object System.Windows.Forms.Padding(0, 8, 0, 4)
    $helperButtons = @(
        @{ Text = 'נקה הכל';    Action = { foreach ($b in $boxes) { $b.Checked = $false } }.GetNewClosure() },
        @{ Text = 'סמן הכל';    Action = { foreach ($b in $boxes) { $b.Checked = $true } }.GetNewClosure() },
        @{ Text = 'הפוך סימון'; Action = { foreach ($b in $boxes) { $b.Checked = -not $b.Checked } }.GetNewClosure() }
    )
    foreach ($spec in $helperButtons) {
        $button = New-Object System.Windows.Forms.Button
        $button.Text = $spec.Text
        $button.AutoSize = $true
        $button.Add_Click($spec.Action)
        $helpers.Controls.Add($button)
    }
    $layout.Controls.Add($helpers)

    $installBox = New-Object System.Windows.Forms.CheckBox
    $installBox.Text = 'גם להתקין על המכשיר אחרי הבנייה (installDebug)'
    $installBox.AutoSize = $true
    $installBox.Checked = $Install
    $installBox.Margin = New-Object System.Windows.Forms.Padding(3, 6, 3, 3)
    $layout.Controls.Add($installBox)

    if ($FreeGB -ge 0) {
        $disk = New-Object System.Windows.Forms.Label
        $disk.AutoSize = $true
        $disk.Margin = New-Object System.Windows.Forms.Padding(3, 6, 3, 3)
        if ($FreeGB -lt $MinFreeGB) {
            $disk.Text = "מקום פנוי בדיסק: $FreeGB GB - מומלץ לפחות $MinFreeGB GB, הבנייה עלולה להיכשל"
            $disk.ForeColor = [System.Drawing.Color]::Firebrick
        } else {
            $disk.Text = "מקום פנוי בדיסק: $FreeGB GB"
            $disk.ForeColor = [System.Drawing.SystemColors]::GrayText
        }
        $layout.Controls.Add($disk)
    }

    $summary = New-Object System.Windows.Forms.Label
    $summary.Font = $boldFont
    $summary.AutoSize = $true
    $summary.Margin = New-Object System.Windows.Forms.Padding(3, 10, 3, 6)
    $layout.Controls.Add($summary)

    $actions = New-Object System.Windows.Forms.FlowLayoutPanel
    $actions.AutoSize = $true
    $actions.AutoSizeMode = [System.Windows.Forms.AutoSizeMode]::GrowAndShrink
    $actions.WrapContents = $false
    $buildButton = New-Object System.Windows.Forms.Button
    $buildButton.Text = 'בנה'
    $buildButton.AutoSize = $true
    $buildButton.Padding = New-Object System.Windows.Forms.Padding(16, 2, 16, 2)
    $buildButton.DialogResult = [System.Windows.Forms.DialogResult]::OK
    $cancelButton = New-Object System.Windows.Forms.Button
    $cancelButton.Text = 'ביטול'
    $cancelButton.AutoSize = $true
    $cancelButton.Padding = New-Object System.Windows.Forms.Padding(16, 2, 16, 2)
    $cancelButton.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
    $actions.Controls.Add($buildButton)
    $actions.Controls.Add($cancelButton)
    $layout.Controls.Add($actions)

    $form.Controls.Add($layout)
    $form.AcceptButton = $buildButton
    $form.CancelButton = $cancelButton
    $form.Add_Shown({ $this.Activate() })

    # מסמן ויזואלית מה לא ייבנה ומעדכן את שורת הסיכום
    $update = {
        $skip = 0
        foreach ($b in $boxes) {
            if ($b.Checked) {
                $skip++
                $b.Font = $strikeFont
                $b.ForeColor = [System.Drawing.SystemColors]::GrayText
            } else {
                $b.Font = $font
                $b.ForeColor = [System.Drawing.SystemColors]::ControlText
            }
        }
        $count = $boxes.Count - $skip
        if ($count -eq 0) {
            $summary.Text = 'לא נשארה אף אפליקציה לבנייה'
        } elseif ($installBox.Checked) {
            $summary.Text = "ייבנו ויותקנו $count מתוך $($boxes.Count) אפליקציות"
        } else {
            $summary.Text = "ייבנו $count מתוך $($boxes.Count) אפליקציות"
        }
        $buildButton.Enabled = $count -gt 0
    }.GetNewClosure()
    foreach ($b in $boxes) { $b.Add_CheckedChanged($update) }
    $installBox.Add_CheckedChanged($update)
    & $update

    return @{ Form = $form; Boxes = $boxes; InstallBox = $installBox; Summary = $summary; BuildButton = $buildButton }
}

# מחזיר $null אם בוטל, אחרת את הבחירה
function Show-Picker {
    param([string[]]$Apps, [string[]]$Excluded, [bool]$Install, [double]$FreeGB)
    $picker = New-PickerForm -Apps $Apps -Excluded $Excluded -Install $Install -FreeGB $FreeGB
    try {
        if ($picker.Form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { return $null }
        return @{
            Excluded = @($picker.Boxes | Where-Object { $_.Checked } | ForEach-Object { $_.Text })
            Install  = $picker.InstallBox.Checked
        }
    } finally {
        $picker.Form.Dispose()
    }
}

# adb לא תמיד ב-PATH; בלעדיו installDebug נכשל בכל אפליקציה בנפרד, אז בודקים מראש
function Find-Adb {
    $command = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $candidates = @()
    if ($env:ANDROID_HOME)     { $candidates += Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }
    if ($env:ANDROID_SDK_ROOT) { $candidates += Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' }
    if ($env:LOCALAPPDATA)     { $candidates += Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return $null
}

# Process עם timeout ולא "& adb": כשפקודת adb מפעילה את השרת שלו, השרת יורש את
# ה-pipe של הפלט, ו-PowerShell מחכה ל-EOF שלא מגיע כל עוד השרת חי - כלומר
# הסקריפט היה נתקע בכל פעם ששרת ה-adb עוד לא רץ (למשל אחרי הפעלה מחדש).
function Invoke-Adb([string]$Adb, [string]$Arguments) {
    $info = New-Object System.Diagnostics.ProcessStartInfo($Adb, $Arguments)
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($info)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    [void]$process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit(30000)) {
        try { $process.Kill() } catch { }
        return ''
    }
    if ($stdout.Wait(3000)) { return $stdout.Result }
    return ''
}

function Test-DeviceConnected([string]$Adb) {
    # קודם מעלים את השרת בנפרד, כדי ש-devices עצמו לא יפעיל אותו ויחזיק את ה-pipe
    [void](Invoke-Adb $Adb 'start-server')
    $lines = (Invoke-Adb $Adb 'devices') -split "`r?`n"
    return [bool]($lines | Where-Object { $_ -match '^\S+\s+device\s*$' })
}

function Format-Duration([TimeSpan]$Span) {
    return '{0}:{1:00}' -f [int][math]::Floor($Span.TotalMinutes), $Span.Seconds
}

function Set-ConsoleTitle([string]$Text) {
    try { $Host.UI.RawUI.WindowTitle = $Text } catch { }
}

function Invoke-Main {
    $apps = @(Get-Apps)
    if ($apps.Count -eq 0) {
        [void](Show-Message -Text "לא נמצאה אף אפליקציה.`nהסקריפט צריך לשבת בתיקייה הראשית של הריפו." -Icon Error)
        $script:ExitCode = 1
        return
    }

    $saved = Read-Selection
    $choice = Show-Picker -Apps $apps -Excluded $saved.Excluded -Install $saved.Install -FreeGB (Get-FreeGB)
    if ($null -eq $choice) {
        $script:ExitCode = 0
        return
    }
    Save-Selection $choice

    $skipped = @($apps | Where-Object { $choice.Excluded -contains $_ })
    $toBuild = @($apps | Where-Object { $choice.Excluded -notcontains $_ })
    $install = $choice.Install

    if ($install) {
        $adb = Find-Adb
        $connected = $false
        if ($adb) {
            Write-Host 'Checking for a connected device...'
            $connected = Test-DeviceConnected $adb
        }
        if (-not $connected) {
            if ($adb) { $reason = 'לא נמצא מכשיר מחובר ב-adb.' } else { $reason = 'adb לא נמצא במחשב.' }
            $answer = Show-Message -Text "$reason`n`nלהמשיך בבנייה בלבד, בלי התקנה?" -Icon Warning -Buttons YesNo
            if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) {
                $script:ExitCode = 0
                return
            }
            $install = $false
        }
    }

    if ($install) { $task = 'installDebug' } else { $task = 'assembleDebug' }
    Write-Host "Building $($toBuild.Count) of $($apps.Count) apps with $task"
    if ($skipped.Count -gt 0) { Write-Host "Skipping: $($skipped -join ', ')" }

    $log = New-Object System.IO.StreamWriter($LogFile, $false, (New-Object System.Text.UTF8Encoding($false)))
    $log.AutoFlush = $true
    $succeeded = @()
    $failed = @()
    $total = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $log.WriteLine("FutureOS build $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - $task")
        $log.WriteLine("Skipped: $($skipped -join ', ')")
        for ($i = 0; $i -lt $toBuild.Count; $i++) {
            $app = $toBuild[$i]
            $progress = "[$($i + 1)/$($toBuild.Count)] $app"
            Set-ConsoleTitle "FutureOS build $progress"
            Write-Host ''
            Write-Host "=== $progress ===" -ForegroundColor Cyan
            $log.WriteLine('')
            $log.WriteLine("=== $progress ===")

            $watch = [System.Diagnostics.Stopwatch]::StartNew()
            # ".\" במפורש: כש-NoDefaultCurrentDirectoryInExePath מוגדר, cmd לא מחפש
            # קבצי הרצה בתיקייה הנוכחית ו-"gradlew.bat" לבד לא נמצא
            $code = [FutureOSBuild.Runner]::Run((Join-Path $Root $app), ".\gradlew.bat $task --console=plain", $log)
            $elapsed = Format-Duration $watch.Elapsed

            if ($code -eq 0) {
                $succeeded += $app
                Write-Host "OK: $app ($elapsed)" -ForegroundColor Green
                $log.WriteLine("OK: $app ($elapsed)")
            } else {
                $failed += $app
                Write-Host "!! FAILED: $app (exit code $code, $elapsed)" -ForegroundColor Red
                $log.WriteLine("!! FAILED: $app (exit code $code, $elapsed)")
            }
        }
    } finally {
        $log.Dispose()
    }
    $duration = Format-Duration $total.Elapsed

    Set-ConsoleTitle 'FutureOS build - done'
    Write-Host ''
    Write-Host "=== Summary ($duration) ===" -ForegroundColor Cyan
    Write-Host "Succeeded ($($succeeded.Count)/$($toBuild.Count)): $($succeeded -join ', ')" -ForegroundColor Green
    if ($failed.Count -gt 0)  { Write-Host "Failed ($($failed.Count)): $($failed -join ', ')" -ForegroundColor Red }
    if ($skipped.Count -gt 0) { Write-Host "Skipped ($($skipped.Count)): $($skipped -join ', ')" }
    Write-Host "Full log: $LogFile"

    if ($install) { $verb = 'נבנו והותקנו' } else { $verb = 'נבנו' }
    $lines = @("הריצה הסתיימה אחרי $duration דקות.", '', "$verb בהצלחה: $($succeeded.Count) מתוך $($toBuild.Count)")
    if ($failed.Count -gt 0)  { $lines += "נכשלו: $($failed -join ', ')" }
    if ($skipped.Count -gt 0) { $lines += "דולגו לפי הבחירה: $($skipped.Count)" }

    if ($failed.Count -gt 0) {
        $lines += @('', 'לפתוח את הלוג המלא?')
        $answer = Show-Message -Text ($lines -join "`n") -Icon Warning -Buttons YesNo
        if ($answer -eq [System.Windows.Forms.DialogResult]::Yes) {
            Start-Process -FilePath 'notepad.exe' -ArgumentList "`"$LogFile`""
        }
        $script:ExitCode = 1
    } else {
        [void](Show-Message -Text ($lines -join "`n"))
        $script:ExitCode = 0
    }
}

# כשהקובץ נטען ב-dot-source (". .\build-apps.ps1") רק הפונקציות נטענות, בלי
# לפתוח חלון - כך אפשר לבדוק אותן בנפרד
if ($MyInvocation.InvocationName -ne '.') {
    $script:ExitCode = 1
    try {
        Invoke-Main
    } catch {
        Write-Host "Unexpected error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host $_.ScriptStackTrace
        $script:ExitCode = 1
    }
    exit $script:ExitCode
}
