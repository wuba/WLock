#!/bin/sh
# ----------------------------------------------------------------------------
# Copyright (C) 2005-present, 58.com.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ----------------------------------------------------------------------------

### ====================================================================== ###
##                                                                         ##
##                       wlock server bootstrap script                     ##
##                                                                         ##
### ====================================================================== ###

CLASS_PATH=""
SERVICE_NAME=wlock
DIR="$(cd "$(dirname "$0")" || exit;pwd)/.."
PID_PATH="$DIR"/tmp/pid
PID_FILE="$PID_PATH"/"$SERVICE_NAME"
sh "$DIR"/bin/rshutdown.sh
mkdir -p "$DIR"/tmp/pid/
mkdir -p "$DIR"/log/gc.log

checkJdkUtil() {
  # check tools.jar
  if [ ! -f "$JAVA_HOME"/lib/tools.jar ]; then
    echo "Can't find tools.jar in JAVA_HOME"
    echo "Need a JDK to run javac"
    exit 1
  fi
}

checkIsRunning() {
  if [ -e "$PID_FILE" ]; then
    PID_INFO=$(cat "$PID_FILE")
    SERVICE_PID=$(pgrep -f "$PID_INFO" | sed -n '1P' | awk '{print $2}')
    echo "ps -ef | grep -v grep | grep $PID_INFO | sed -n  '1P' | awk '{print $2}'"
    echo "$SERVICE_PID"
    if [ "$SERVICE_PID" ]; then
      echo "startup fail! Please close the service after to restart!"
      echo "$(date)" +"[$SERVICE_NAME] is running" >>server/log/monitor.log
      exit 1
    else
      echo "This service will startup!"
      echo "$(date)" +"[$SERVICE_NAME] is starting" >>server/log/monitor.log
    fi
  fi
  # check service is run
  JAVA_COUNT=$(pgrep -f | grep java | grep -c "sn:$SERVICE_NAME")
  if [ "$JAVA_COUNT" -ge 1 ]; then
    echo "warning: has a [$SERVICE_NAME] is running, please check......................................"
    exit 1
  fi
}

initClassPath() {
  # class path
  CLASS_PATH=.:"$JAVA_HOME"/lib/tools.jar
  for jar in "$DIR"/lib/*.jar; do
    CLASS_PATH=$CLASS_PATH:$jar
  done
}

main() {
  checkJdkUtil
  checkIsRunning PID_FILE
  initClassPath
  MAIN_CLASS=com.wuba.wlock.server.bootstrap.Main
  java -Xmx8g -Xms8g -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=2 -XX:+PrintClassHistogram -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:server/log/gc.log -XX:SurvivorRatio=8 -XX:MaxGCPauseMillis=200 -classpath "$CLASS_PATH" -Duser.dir="$DIR" $MAIN_CLASS >>/dev/null 2>&1 &
  echo pid:$!
  echo $! >"$PID_PATH"/"$SERVICE_NAME"
}

main
