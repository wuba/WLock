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

USAGE="Usage: quickinit <other_config>
	   other_config: <sequence_id> <ip> <tcp_port> <paxos_port> <udp_port> <token>
Example:
	  quickinit <sequence_id> <ip> <tcp_port> <paxos_port> <udp_port> <token>
	  quickinit <sequence_id> <ip> <tcp_port> <paxos_port> <udp_port>"

SEQUENCE_ID=""
IP=""
TCP_PORT=""
PAXOS_PORT=""
UDP_PORT=""
TOKEN="123"

checkParam() {
  if [ "$SEQUENCE_ID" = "" ]; then
    echo "sequence_id must not null"
    exit 1
  fi
  if [ "$IP" = "" ]; then
    echo "ip must not null"
    exit 1
  fi
  if [ "$TCP_PORT" = "" ]; then
    echo "tcp_port must not null"
    exit 1
  fi
  if [ "$PAXOS_PORT" = "" ]; then
    echo "paxos_port must not null"
    exit 1
  fi
  if [ "$UDP_PORT" = "" ]; then
    echo "udp_port must not null"
    exit 1
  fi
}

initParam() {
  SEQUENCE_ID=$1
  IP=$2
  TCP_PORT=$3
  PAXOS_PORT=$4
  UDP_PORT=$5
  if [ $# = 6 ]; then
    TOKEN=$6
  fi
  echo "use <sequence_id> [$SEQUENCE_ID] <ip> [$IP] <tcp_port> [$TCP_PORT] <paxos_port> [$PAXOS_PORT] <udp_port> [$UDP_PORT] <token> [$TOKEN] "
}

main() {
  echo "$*"
  initParam $*
  checkParam $*
  echo "{\"sequenceId\":$SEQUENCE_ID,\"ip\":\"$IP\",\"tcpPort\":$TCP_PORT,\"paxosPort\":$PAXOS_PORT,\"udpPort\":$UDP_PORT}"
  curl -X POST "http://localhost:8888/wlock/quick/init" -H "accept: */*" -H "token: $TOKEN" -H "Content-Type: application/json" -d "{\"sequenceId\":$SEQUENCE_ID,\"ip\":\"$IP\",\"tcpPort\":$TCP_PORT,\"paxosPort\":$PAXOS_PORT,\"udpPort\":$UDP_PORT}"
}

if [ $# -lt 1 ]; then
  echo "$USAGE"
  exit 1
fi

main "$*"
