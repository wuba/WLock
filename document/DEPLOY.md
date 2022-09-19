


# 服务端初始化详细步骤 : 

> 创建集群,添加节点,节点上线相关操作都可以通过 UI 快速完成 : http://localhost:8888/swagger-ui/index.html
> url 中的 localhost 表示的是注册中心地址

## 常规服务部署与启动

### 创建数据表 [相关 SQL](sql/create.sql)
### 调整数据库配置
```
cd target
unzip registry.zip
cd registry
# 编辑配置文件 : 
#		设置配置中的数据库 url 以及用户名,密码.
#		设置配置中的redis ip 端口以及密码.
vi config/application.yml
```
### 部署注册中心并启动

```shell
sh bin/start.sh
```
### 通过swagger进行集群创建  

接口 : `/wlock/cluster/add`

### 通过swagger进行节点添加  

> wlock 服务端支持集群模式部署和单机部署,如果服务端为单机部署,在添加节点时候只要添加一个节点即可.
>
> 如果是集群模式部署,请注意多个集群使用的paxos 端口  , tcp 端口  , telnet 端口,udp 端口需要保持一致,每个节点的序列 ID 需要不一样.

接口 : `/wlock/server/add`

### 通过swagger进行节点上线  
> 对于添加好的节点进行上线操作,只有上线后的节点才会真正对外提供服务

接口 : `/wlock/server/online`


### 服务端初始化
> 按照添加节点进行项目启动 ,启动节点数量和添加节点数量相同

```shell
# 1. 执行初始化之前请确认 config 下的registry.properties 中的 registryServerIp配置是不是注册中心 ip
# 2. 确认 server.properties 配置的 listenPort 是不是注册中心新增节点的 tcp 端口,二者需要保持一致
# 3. 由于服务端使用了 RocksDB,mac 的m1 芯片不支持运行 RocksDB,所以建议服务端部署服务器运行
cd target
unzip -d ./server server.zip
sh server/bin/start.sh
```

