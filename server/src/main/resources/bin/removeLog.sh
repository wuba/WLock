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
#! /bin/bash

dir="$(cd "$( dirname "$0" )" && pwd)"
rootpath="$(cd "$dir/.." && pwd)"
THEDAY1=`date -d "1 days ago" +%Y-%m-%d`
THEDAY5=`date -d "3 days ago" +%Y-%m-%d`


LogPath=$rootpath/log

rm -f $LogPath/wlock-$THEDAY5-1.log
rm -f $LogPath/trace-$THEDAY1-1.log


