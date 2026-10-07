@echo off
rem Ejecutador CLI para LIT
java -cp "%~dp0target\classes" com.lit.cli.LitCli %*
