# Determine the root path based on the OS
$rootPath = if ($PSVersionTable.Platform -eq "Unix") { "/" } else { [System.IO.Path]::GetPathRoot($env:SystemRoot) }

# Make sure the script is running on PowerShell 7 or later
$majorVersion = $PSVersionTable.PSVersion.Major

if ($majorVersion -ge 7) {
    Write-Host "Running in PowerShell 7 or higher (version $majorVersion)."
}
else {
    Write-Host "Running in Windows PowerShell (version $majorVersion)."

    try {
        $output = pwsh -Command { $PSVersionTable.PSVersion.Major }

        if ($output -eq 7) {
            Write-Host "PowerShell 7 is installed. Switching to PowerShell 7..."
        }
    }
    catch {
        Write-Host "PowerShell 7 is not installed."
        $command = "winget install --id Microsoft.Powershell --source winget"
        iex $command
        Write-Host "Installation complete."
        if ($host.UI.RawUI -and $host.UI.RawUI.SupportsVirtualTerminal) { Clear-Host }
    }

    $scriptPath = $MyInvocation.MyCommand.Path

    # Run the script with PowerShell 7
    $command = "pwsh -File $scriptPath"
    iex $command
    exit
}

# Function to get folder sizes
function Get-FolderSize {
    param (
        [string]$folderPath
    )

    $size = (Get-ChildItem $folderPath -Recurse -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum

    [PSCustomObject]@{
        "Folder" = $folderPath
        "Size" = $size
    }
}

if ($host.UI.RawUI -and $host.UI.RawUI.SupportsVirtualTerminal) { Clear-Host }

# Get all top-level folders on the root path
$folders = Get-ChildItem -Path $rootPath -Directory -ErrorAction SilentlyContinue
$throttleLimit = [Environment]::ProcessorCount

# Get the total number of folders
$totalFolders = $folders.Count
$progressCounter = 0

# Use ForEach-Object
$measure = Measure-Command {
    $folderSizes = $folders | ForEach-Object {
        $folder = $_

        # Calculate the size of the folder
        $size = (Get-ChildItem $folder.FullName -Recurse -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum

        # Increment the progress counter atomically
        $progressCounter++

        # Calculate the percentage of progress
        $progressPercentage = ($progressCounter / $totalFolders) * 100

        # Write the progress to the host
        Write-Host "PROGRESS:$progressPercentage" -NoNewLine
        Write-Host "`r" -NoNewline

        # Return an object with the folder name and size
        return [PSCustomObject]@{
            "Folder" = "FOLDER:" + $folder.FullName
            "Size" = $size
        }
    }
}
$timeTaken = @()

if ($measure.Days -gt 0) {
    $timeTaken += "$($measure.Days) Days"
}

if ($measure.Hours -gt 0) {
    $timeTaken += "$($measure.Hours) Hours"
}

if ($measure.Minutes -gt 0) {
    $timeTaken += "$($measure.Minutes) Minutes"
}

if ($measure.Seconds -gt 0) {
    $timeTaken += "$($measure.Seconds) Seconds"
}

Write-Host ("Time taken: " + ($timeTaken -join ", "))


# Now $folderSizes contains an array of custom objects with the folder names and sizes
$driveName = $rootPath.TrimEnd('\').TrimEnd(':')
$driveInfo = Get-PSDrive -Name $driveName
$totalSize = $driveInfo.Used + $driveInfo.Free

# Sort the folder sizes and select the top 10
$sortedFolderSizes = $folderSizes | Sort-Object -Property Size -Descending | Select-Object -First 10

# Print the size percentages
$sortedFolderSizes | ForEach-Object {
    Write-Host ("SIZE:{0:N2}" -f (($_.Size / $totalSize) * 100))
}

# Print the folder names
$sortedFolderSizes | ForEach-Object {
    Write-Host ($_.Folder)
}
exit


