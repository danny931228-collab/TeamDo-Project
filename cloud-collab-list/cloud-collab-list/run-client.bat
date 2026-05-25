@echo off
chcp 65001 > nul
echo 啟動 雲端共編清單 Client...
mvn -q javafx:run
pause
