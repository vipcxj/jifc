@echo off
set VERSION=0
set VSPATH=
for /f "tokens=1,2*" %%a in ('reg query "HKLM\SOFTWARE\Wow6432Node\Microsoft\VisualStudio\SxS\VS7" 2^>nul') do (
	if "%%c" NEQ "" (
		if %%a GTR %VERSION% (
			set VERSION=%%a
			set VSPATH=%%c
		)
	)
)
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set OS=x32 || set OS=x64

if %JAVACPP_TARGET_PLATFORM%==x86 (
	set arch=x86
	GOTO x86
)
if %JAVACPP_TARGET_PLATFORM%==x86_64 (
	set arch=amd64
	GOTO x64
)

echo "Invalid argument: %JAVACPP_TARGET_PLATFORM%"
exit

:x86
if %OS%==x32 (
	set host_arch=x86
	set VS_OPT=x86
) else (
	set host_arch=amd64
	set VS_OPT=amd64_x86
)
	
:x64
if %OS%==x32 (
	set VS_OPT=x86_amd64
) else (
	set VS_OPT=amd64
)

if exist "%VSPATH%\vsvarsall.bat" (
	call "%VSPATH%Common7\Tools\vsvarsall.bat" %VS_OPT%
) else (
	set VSCMD_DEBUG=0
	call "%VSPATH%Common7\Tools\VsDevCmd.bat" -arch=%arch% -host_arch=%host_arch% > nul
)

cl %*