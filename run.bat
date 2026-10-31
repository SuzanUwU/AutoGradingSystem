@echo off
cd /d "%~dp0"

set "LIB_CP=lib\pdfbox-3.0.8.jar;lib\fontbox-3.0.8.jar;lib\pdfbox-io-3.0.8.jar;lib\commons-logging-1.4.0.jar"

java -cp "classes;%LIB_CP%" autograder.Main