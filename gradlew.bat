@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@echo off
@rem Giữ nguyên đường dẫn tiếng Việt của workspace khi chạy trong Windows Terminal.
@chcp 65001 > nul
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions EnableDelayedExpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Gradle tạo tiến trình test bằng classpath; trên Windows PowerShell 5.1, đường
@rem dẫn có dấu bị đổi mã. Ánh xạ tạm sang W: rồi tự chạy lại để mọi tác vụ
@rem (kể cả unit test) hoạt động bằng đúng một lệnh .\gradlew.bat.
if not defined MATHWAKE_MAPPED (
    subst W: "%APP_HOME%" > nul 2>&1
    if exist W:\gradlew.bat (
        set MATHWAKE_MAPPED=1
        pushd W:\
        call W:\gradlew.bat %*
        set MATHWAKE_RESULT=!ERRORLEVEL!
        popd
        subst W: /d > nul 2>&1
        exit /b !MATHWAKE_RESULT!
    )
)

@rem Java 17 nhận sai đối số chứa dấu từ Windows PowerShell 5.1. Chạy wrapper từ
@rem một đường dẫn ASCII tạm, còn thư mục dự án và mọi đầu ra vẫn giữ nguyên.
set WRAPPER_HOME=%TEMP%\MathWakeWrapper\gradle\wrapper
if not exist "%WRAPPER_HOME%" mkdir "%WRAPPER_HOME%"
copy /Y "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" "%WRAPPER_HOME%\gradle-wrapper.jar" > nul
copy /Y "%APP_HOME%\gradle\wrapper\gradle-wrapper.properties" "%WRAPPER_HOME%\gradle-wrapper.properties" > nul

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:execute
@rem Setup the command line



@rem Execute Gradle
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%WRAPPER_HOME%\gradle-wrapper.jar" %* & call :exitWithErrorLevel

:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
