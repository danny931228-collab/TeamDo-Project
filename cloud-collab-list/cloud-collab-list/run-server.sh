#!/usr/bin/env bash
set -e
echo "啟動 雲端共編清單 Server..."
mvn -q exec:java -Dexec.mainClass=com.cloudlist.server.CloudListServer -Dexec.args="5555"
