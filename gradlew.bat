@echo off
set DIR=%~dp0
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%in\java.exe
) else (
  set JAVA_EXE=java
)
"%JAVA_EXE%" -jar "%DIR%gradle\wrapper\gradle-wrapper.jar" %*
