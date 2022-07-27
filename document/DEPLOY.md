


# 服务端初始化详细步骤 : 

> 创建集群,添加节点,节点上线相关操作都可以通过 UI 快速完成 : http://localhost:8888/swagger-ui/index.html
> 		下方列出的方式是使用 curl 直接调用接口

1. **项目打包 :** 
```shell
mvn clean install
```
2. **创建数据表** - [相关 SQL](document/sql/create.sql)
3. **部署注册中心并启动**
```shell
cd target
sh registry/start.sh
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
# 	需要上线的服务序列 id 列表
curl -X POST "http://localhost:8888/wlock/server/online?clusterName=demo_01&serverIdList=1" -H "accept: */*" -H "token: token" -d ""
```
7. **服务端初始化**
> 按照添加节点进行项目启动 ,启动节点数量和添加节点数量相同

```shell
cd target
sh server/start.sh
```