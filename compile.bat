@echo off
setlocal EnableDelayedExpansion

cd /d "%~dp0"

if not exist classes mkdir classes

set "SOURCE_LIST=%TEMP%\autograder_sources_%RANDOM%_%RANDOM%.txt"

if exist "%SOURCE_LIST%" del /q "%SOURCE_LIST%"

for /r "src" %%F in (*.java) do (
    set "FILE=%%F"
    set "FILE=!FILE:\=/!"
    echo "!FILE!">>"%SOURCE_LIST%"
)

javac -d classes @"%SOURCE_LIST%"

set "RESULT=%ERRORLEVEL%"

del /q "%SOURCE_LIST%"

if not "%RESULT%"=="0" (
    echo Compilation failed.
    exit /b %RESULT%
)

echo Compilation successful.