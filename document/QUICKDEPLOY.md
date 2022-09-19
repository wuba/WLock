


# 服务端初始化详细步骤 : 

> 创建集群,添加节点,节点上线相关操作都可以通过 UI 快速完成 : http://localhost:8888/swagger-ui/index.html
> url 中的 localhost 表示的是注册中心地址

## 快速部署与启动
### 部署注册中心并启动  
```shell
# 在 target 目录中找到 registry 目录,cd 到该目录下执行下面命令 : 
sh bin/start.sh
# 确认是否启动成功  : 
cat log/registry* | grep "Application start finish"
```
### 快速初始化服务节点  
#### swagger 方式 : 

快速启动接口 : `/wlock/quick/init`

备注 : 该接口会自动帮助创建 `default_cluster`集群,并且按照参数添加节点,对节点进行创建,给该集群添加秘钥`default_key`.秘钥key和集群会在响应中返回.

#### 脚本方式 : 

```shell
# sequence_id 序列 id,用于唯一标识一个集群内的节点,同一集群内唯一
# ip 节点 ip
# tcp_port : 用于对外暴露的端口
# paxos_port : 进行 paxos 的端口
# udpPort : paxos 进行 udp 通信端口
sh quickStart.sh quickinit <sequence_id> <ip> <tcp_port>  <paxos_port> <udp_port>
```
### 启动服务节点  
> 依次启动已添加的服务节点

```shell
# 启动前确认项：
# 1. 执行初始化之前请确认 config 下的registry.properties 中的 registryServerIp配置是不是注册中心 ip
# 2. 确认 server.properties 配置的 listenPort 是不是注册中心新增节点的 tcp 端口,二者需要保持一致
# 3. 由于服务端使用了 RocksDB,mac 的m1 芯片不支持运行 RocksDB,所以建议服务端部署服务器运行
cd target
unzip -d ./server server.zip
sh server/bin/start.sh
```