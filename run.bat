@echo off

echo Before changing dir: %cd%
cd /d "%~dp0\target"
echo After changing dir: %cd%

java -jar intelligent-report-generator-0.0.1-SNAPSHOT.jar

pause

cd ..
echo After cd ..: %cd%

pause
