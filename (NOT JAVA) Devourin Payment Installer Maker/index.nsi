!include MUI2.nsh
!include nsDialogs.nsh

; Settings - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
Unicode true
Name "Devourin Payment"
OutFile "devourin_payment_installer.exe"
RequestExecutionLevel admin
InstallDir "$PROGRAMFILES64\Devourin\"
!define MUI_ICON ".\devourin.ico"
!define MUI_HEADERIMAGE
; !define MUI_HEADERIMAGE_BITMAP "path\to\InstallerLogo.bmp"
!define MUI_HEADERIMAGE_RIGHT

; Vars - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
Var dialog
Var errorText

; Terminal ID
Var label_terminalId
Var terminalId

; Server Address
Var label_serverAddress
Var serverAddress

; Java Home
Var javaHome
Var defaultJavaHome

; Pages  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

!define MUI_WELCOMEPAGE_TITLE "Devourin Payment Service"
!define MUI_WELCOMEPAGE_TEXT "This installer will install the Devourin payment service to your PC.$\n$\nPlease make sure to follow the install steps correctly so that this software works as intended."
!insertmacro MUI_PAGE_WELCOME

Page custom pageGetEnvVars pageCheckEnvVars

!define MUI_PAGE_HEADER_SUBTEXT "Choose the folder in which to install the service."
!define MUI_DIRECTORYPAGE_TEXT_TOP "The installer will install the service in the following folder. To install in a different folder, click Browse and select another folder. Click Next to continue."
!insertmacro MUI_PAGE_DIRECTORY

!insertmacro MUI_PAGE_INSTFILES

; Language (has to be set after pages) - - - - - - - - - - - - - - - - - - -
!insertmacro MUI_LANGUAGE "English"

; Functions  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

; Set variables on initialisation
Function .onInit
    StrCpy $terminalId "1"
    StrCpy $serverAddress "http://localhost:8080/nebula-services-1.6/devsync"
    StrCpy $javaHome ""
    
    ReadEnvStr $defaultJavaHome "JAVA_HOME"
    ${If} "$defaultJavaHome" == ""
        StrCpy $defaultJavaHome "$PROGRAMFILES64\"
    ${EndIf}
FunctionEnd

; ENV_VARIABLES_START

Function pageGetEnvVars
    nsDialogs::Create /NOUNLOAD 1018
    Pop $dialog
    ${If} $dialog == error
        Abort
    ${EndIf}

    var /global terminalId_text
    var /global serverAddress_text

    ${NSD_CreateLabel} 0 0 100% 12u "Enter terminal ID (from branch_terminal table):"
    Pop $label_terminalId
    ${NSD_CreateText} 0 13u 100% 12u "$terminalId"
    Pop $terminalId_text
    ${NSD_OnChange} $terminalId_text onTerminalIdTextChange

    ${NSD_CreateLabel} 0 26u 100% 12u "Enter address for Devourin server:"
    Pop $label_serverAddress
    ${NSD_CreateText} 0 39u 100% 12u "$serverAddress"
    Pop $serverAddress_text
    ${NSD_OnChange} $serverAddress_text onServerAddressTextChange

    nsDialogs::Show
FunctionEnd

Function pageCheckEnvVars
    ${If} "$terminalId" == ""
        MessageBox MB_ICONEXCLAMATION "The Terminal ID was empty."
        Abort
    ${ElseIf} "$serverAddress" == ""
        MessageBox MB_ICONEXCLAMATION "The Server Address was empty."
        Abort
    ${EndIf}
FunctionEnd

Function onTerminalIdTextChange
    ${NSD_GetText} $terminalId_text $terminalId
FunctionEnd

Function onServerAddressTextChange
    ${NSD_GetText} $serverAddress_text $serverAddress
FunctionEnd

Function pageGetJavaDir
    !insertmacro MUI_HEADER_TEXT "Choose Java Home Folder" "Choose the folder where 'bin/java.exe' exists."


    nsDialogs::Create 1018
    Pop $0
    ${If} $0 == error
        Abort
    ${EndIf}

    var /global browseButton
    var /global directoryText

    ${If} $javaHome == ""
        StrCpy $javaHome "$defaultJavaHome"
    ${EndIf}

    ${NSD_CreateLabel} 0 0 100% 36u "The following folder will be used as the Java Home. To use a different folder, click Browse and select another folder. Click Next to continue."
    Pop $0 ; ignore

    ${NSD_CreateText} 0 37u 75% 12u "$javaHome"
    pop $directoryText
    ${NSD_OnChange} $directoryText onJavaDirTextChange

    ;create button, save handle and connect to function
    ${NSD_CreateBrowseButton} 80% 36u 20% 14u "Browse..."
    pop $browseButton
    ${NSD_OnClick} $browseButton onJavaDirButtonClick

    nsDialogs::Show
FunctionEnd

Function onJavaDirButtonClick
    nsDialogs::SelectFolderDialog "Select Java Home Location" "$javaHome"
    Pop $0
    ${If} $0 != error
        StrCpy $javaHome $0
        ${NSD_SetText} $directoryText $javaHome
    ${EndIf}
FunctionEnd

Function onJavaDirTextChange
    ${NSD_GetText} $directoryText $javaHome
FunctionEnd

Function pageCheckJavaDir
    ${IfNot} ${FileExists} "$javaHome\bin\java.exe"
        MessageBox MB_ICONEXCLAMATION "This is not a valid Java path."
        MessageBox MB_ICONEXCLAMATION '"$InstDir\nssm" install "Devourin Printing" "$javaHome\bin\java.exe" -jar "$InstDir\Printing\devourin-printing.jar"'
        Abort
    ${EndIf}
FunctionEnd

; ENV_VARIABLES_END

; Sections - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
Section
    ExecDos::exec /TIMEOUT=2000 '"$InstDir\nssm" stop "Devourin Payment"'
    Pop $0

    ExecDos::exec /TIMEOUT=2000 '"$InstDir\nssm" remove "Devourin Payment" confirm'
    Pop $0

    SetOutPath "$InstDir\Payment"

    File ".\devourin-payment.jar"

    SetOutPath $InstDir

    ${IfNot} ${FileExists} "$InstDir\nssm.exe"
        File ".\nssm.exe"
    ${EndIf}

    ; Executes the specified command in cmd
    ExecDos::exec /TIMEOUT=2000 'setx /m DEVOURIN_SERVER_URL "$serverAddress"'
    Pop $0

    ExecDos::exec /TIMEOUT=2000 'setx /m DEVOURIN_TERMINAL "$terminalId"'
    Pop $0

    ExecDos::exec /TIMEOUT=2000 '"$InstDir\nssm" install "Devourin Payment" "$javaHome\bin\java.exe" -jar "$InstDir\Payment\devourin-payment.jar"'
    Pop $0

    ExecDos::exec /TIMEOUT=2000 '"$InstDir\nssm" start "Devourin Payment"'
    Pop $0
SectionEnd