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
#!/bin/sh

### ====================================================================== ###
##                                                                          ##
##                       wlock server bootstrap script                        ##
##                                                                          ##
### ====================================================================== ###




source /etc/profile
. JDK jdk1.8.0_66
USAGE="Usage: startup.sh"
SYSTEM_PROPERTY=""

cd $(dirname "$0")

# 启动之前 先执行 rshutdown.sh
sh ./rshutdown.sh $0 $1 $2 $3 $4

# get arguments
dir="$(cd "$( dirname "$0" )" && pwd)"
rootpath="$(cd "$dir/.." && pwd)"
SERVICE_NAME=$1

#if this service is not shutwodn Please shutdown
SERVICE_DIR=`dirname "$0"`
PORT="$3"
SERVICE_DIR=`cd "$bin"; pwd`

PID_FILE="$SERVICE_DIR"/../tmp/pid/"$SERVICE_NAME"

mkdir -p "$SERVICE_DIR"/../tmp/pid/

if [ -e $PID_FILE ]; then
  PID_INFO=`cat $PID_FILE`
  SERVICE_PID=`ps -ef | grep -v grep | grep $PID_INFO | sed -n  '1P' | awk '{print $2}'`
  echo "ps -ef | grep -v grep | grep $PID_INFO | sed -n  '1P' | awk '{print $2}'"
  echo $SERVICE_PID
  if [ $SERVICE_PID ]; then
    echo "startup fail! Please close the service after to restart!"
    echo `date` +"[$SERVICE_NAME] is running" >> ../log/monitor.log
    exit 1
  else
    echo "This service will startup!"
    echo `date` +"[$SERVICE_NAME] is starting" >> ../log/monitor.log
  fi
fi

# check tools.jar
if [ ! -f "$JAVA_HOME"/lib/tools.jar ]; then
  echo "Can't find tools.jar in JAVA_HOME"
  echo "Need a JDK to run javac"
  exit 1
fi

# check service is run
javacount=`ps -ef|grep java|grep "sn:$SERVICE_NAME" |wc -l`
#echo "javacount:"$javacount
if [ $javacount -ge 1 ] ; then
  echo "warning: has a [$SERVICE_NAME] is running, please check......................................"
  exit 1
fi

# get path
DIR="bin"
if [ "$DIR" = "bin" ]; then
  DIR=`dirname "$0"`
  DIR=`cd "$bin"; pwd`
fi

PROGNAME=`basename $0`
ROOT_PATH="$DIR"/..
PID_PATH="$ROOT_PATH"/tmp/pid

# java opts
if [ "$VM_XMS" = "" ]; then
  VM_XMS=8g
fi

if [ "$VM_XMX" = "" ]; then
  VM_XMX=8g
fi

if [ "$VM_XMN" = "" ]; then
  VM_XMN=5g
fi

JAVA_OPTS="-Xms$VM_XMS -Xmx$VM_XMX -Xmn$VM_XMN -Xss1024K -XX:PermSize=256m -XX:MaxPermSize=512m -XX:ParallelGCThreads=20 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=15 -XX:CMSInitiatingOccupancyFraction=80 -XX:+PrintClassHistogram -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:../log/gc.log"

# class path
CLASS_PATH=.:"$JAVA_HOME"/lib/tools.jar

for jar in $ROOT_PATH/lib/*.jar; do
  CLASS_PATH=$CLASS_PATH:$jar
done

# main class
MAIN_CLASS=com.wuba.wlock.server.bootstrap.Main


java -Xmx8g -Xms8g -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=2 -XX:+PrintClassHistogram -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:../log/gc.log -XX:SurvivorRatio=8 -XX:MaxGCPauseMillis=200 -classpath $CLASS_PATH -Duser.dir=$DIR -Dport=$PORT $SYSTEM_PROPERTY $MAIN_CLASS >> /dev/null 2>&1 &

echo pid:$!

echo $! > "$PID_PATH"/"$SERVICE_NAME"

