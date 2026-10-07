@echo off
rem Ejecutador CLI para LIT
java -cp "%~dp0target\classes;%~dp0target\dependency\*" com.lit.cli.LitCli %*
