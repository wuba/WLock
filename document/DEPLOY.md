


# 服务端初始化详细步骤 : 

> 创建集群,添加节点,节点上线相关操作都可以通过 UI 快速完成 : http://localhost:8888/swagger-ui/index.html
> 		下方列出的方式是使用 curl 直接调用接口


1. **创建数据表** - [相关 SQL](sql/create.sql)
2. **调整数据库配置**
```
cd target
unzip registry.zip
cd registry
# 编辑配置文件 : 
#		设置配置中的数据库 url 以及用户名,密码.
#		设置配置中的redis ip 端口以及密码.
vi config/application.yml
```
3. **部署注册中心并启动**

```shell
sh bin/start.sh
```
4. **创建集群**


```shell
#  url参数 : 
# 		集群名 
# 		分组数量
curl -X POST "http://localhost:8888/wlock/cluster/add" -H "accept: */*" -H "token: token" -H "Content-Type: application/json" -d "{\"clusterName\":\"demo_01\",\"groupCount\":15}"
```

5. **添加节点 : ** 

> wlock 服务端支持集群模式部署和单机部署,如果服务端为单机部署,在添加节点时候只要添加一个节点即可.
>
> 如果是集群模式部署,请注意多个集群使用的paxos 端口  , tcp 端口  , telnet 端口,udp 端口需要保持一致,每个节点的序列 ID 需要不一样.

```shell
#  url参数 : 
# 		集群名 
# 		节点 
# 		paxos 端口 
# 		序列 id 
# 		tcp 端口 
# 		telnet 端口 
# 		udp 端口
curl -X POST "http://localhost:8888/wlock/server/add?clusterName=demo_01&ip=127.0.0.1&paxosPort=123&sequenceId=1&tcpPort=124&telnetPort=125&udpPort=126" -H "accept: */*" -H "token: token" -d ""
```
6. **节点上线**
> 对于添加好的节点进行上线操作,只有上线后的节点才会真正对外提供服务

```shell
#  url参数 : 
# 	集群名 
# 	需要上线的服务序列 id 列表 : 注意 上线时候使用的节点 id 通过 list 查询集群节点,使用返回的主键 id 作为该参数 ,多个节点使用逗号分割
curl -X POST "http://localhost:8888/wlock/server/online?clusterName=demo_01&serverIdList=1" -H "accept: */*" -H "token: token" -d ""
```
7. **服务端初始化**
> 按照添加节点进行项目启动 ,启动节点数量和添加节点数量相同

```shell
# 1. 执行初始化之前请确认 config 下的registry.properties 中的 registryServerIp配置是不是注册中心 ip
# 2. 确认 server.properties 配置的 listenPort 是不是注册中心新增节点的 tcp 端口,二者需要保持一致
cd target
unzip -d ./server server.zip
sh server/bin/start.sh
```