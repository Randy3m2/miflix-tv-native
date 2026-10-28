#ifndef AppVersion
#define AppVersion "2.0.0-rc1"
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
SetupIconFile=..\..\brunio-pc\assets\brunio.ico
Compression=lzma2
SolidCompression=yes
CloseApplications=yes
WizardStyle=modern
LicenseFile=..\..\brunio-pc\LICENSE.txt
[InstallDelete]
Type: filesandordirs; Name: "{app}\_internal"
[Files]
Source: "..\compose\build\compose\binaries\main\app\BruniO-PC\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\BruniO PC"; Filename: "{app}\BruniO-PC.exe"
Name: "{autodesktop}\BruniO PC"; Filename: "{app}\BruniO-PC.exe"
[Run]
Filename: "{app}\BruniO-PC.exe"; Description: "Abrir BruniO"; Flags: nowait postinstall skipifsilent
