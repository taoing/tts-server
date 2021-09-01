#!/bin/bash
#发布tts-server, jar包上传到 ./upload 目录

set -x
set -e

uploadDir=./upload
jarfile=tts-server-1.0.0.jar

#kill进程
if [[ $(ps -ef | grep "${jarfile}" | grep -v "grep") != "" ]]; then
  ps -ef | grep "${jarfile}" | grep -v "grep" | awk '{print $2}' | xargs kill
fi
sleep 2
if [ -f ./$jarfile ]; then
	rm -rf ./$jarfile
fi
cp -f $uploadDir/$jarfile ./$jarfile

nohup java -jar -Dspring.profiles.active=test $jarfile > /dev/null 2>&1 &
echo "${jarfile} 发布完成"
