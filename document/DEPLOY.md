


# 服务端初始化详细步骤 : 

> 创建集群,添加节点,节点上线相关操作都可以通过 UI 快速完成 : http://localhost:8888/swagger-ui/index.html
> url 中的 localhost 表示的是注册中心地址

## 快速启动

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



## 常规部署集群

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
4. **通过 swagger 进行集群创建**

接口 : `/wlock/cluster/add`

4. **通过 swagger 进行节点添加** 

> wlock 服务端支持集群模式部署和单机部署,如果服务端为单机部署,在添加节点时候只要添加一个节点即可.
>
> 如果是集群模式部署,请注意多个集群使用的paxos 端口  , tcp 端口  , telnet 端口,udp 端口需要保持一致,每个节点的序列 ID 需要不一样.

接口 : `/wlock/server/add`

6. **通过 swagger 进行节点上线**
> 对于添加好的节点进行上线操作,只有上线后的节点才会真正对外提供服务

接口 : `/wlock/server/online`


7. **服务端初始化**
> 按照添加节点进行项目启动 ,启动节点数量和添加节点数量相同

```shell
# 1. 执行初始化之前请确认 config 下的registry.properties 中的 registryServerIp配置是不是注册中心 ip
# 2. 确认 server.properties 配置的 listenPort 是不是注册中心新增节点的 tcp 端口,二者需要保持一致
cd target
unzip -d ./server server.zip
sh server/bin/start.sh
```