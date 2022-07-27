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
##                       SCF server shutdown script                         ##
##                                                                          ##
### ====================================================================== ###


USAGE="Usage: shutdown.sh"

# get arguments
dir="$(cd "$( dirname "$0" )" && pwd)"
rootpath="$(cd "$dir/.." && pwd)"
SERVICE_NAME=`echo $rootpath | awk -F "/" '{print $NF}'`

DIR=`dirname "$0"`
DIR=`cd "$bin"; pwd`
PID_FILE="$DIR"/../tmp/pid/$SERVICE_NAME

if [ ! -e $PID_FILE ]; then
  echo "pid file($PID_FILE) not exits"
  exit 1
fi

echo "kill pid: `cat $PID_FILE`"
kill -s USR2 `cat $PID_FILE`
rm -rf $PID_FILE

