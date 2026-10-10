#ifndef AppVersion
#define AppVersion "1.0.0-rc2"
#endif
[Setup]
AppId={{2E5AFBD5-987C-4783-A75D-643AE766BA95}
AppName=BruniO PC
AppVersion={#AppVersion}
AppPublisher=BruniO
DefaultDirName={localappdata}\Programs\BruniO
DefaultGroupName=BruniO
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
OutputDir=..\installer
OutputBaseFilename=BruniO-PC-Setup-{#AppVersion}
SetupIconFile=..\assets\brunio.ico
Compression=lzma2
SolidCompression=yes
CloseApplications=yes
WizardStyle=modern
LicenseFile=..\LICENSE.txt
[Files]
Source: "..\dist\BruniO-PC\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\BruniO PC"; Filename: "{app}\BruniO-PC.exe"
Name: "{autodesktop}\BruniO PC"; Filename: "{app}\BruniO-PC.exe"
[Run]
Filename: "{app}\BruniO-PC.exe"; Description: "Open BruniO"; Flags: nowait postinstall skipifsilent
