# WLock
WLock是一套基于一致性算法组件[WPaxos](https://github.com/wuba/WPaxos) 实现的高可靠、高吞吐分布式锁服务，可应用于分布式环境下协调多进程/线程对共享资源的访问控制、多节点Master选主等业务场景。

## 核心能力
- **丰富的锁类型：** 互斥锁、可重入锁、公平锁、优先级锁、读写锁；  
- **灵活的锁操作：** 支持阻塞/非阻塞、同步/异步watch等方式获取锁，支持锁续约，TTL等机制；  
- **获取锁粒度:** 进程、线程；  
- **多租户隔离：** 提供秘钥作为集群分配、锁操作隔离、权限控制的租户单位，支持单个秘钥跨集群动态平滑迁移；  
- **配置管理：** 提供集群节点扩/缩容，主从切换配置变更推送，客户端秘钥配置管理，秘钥归属集群平滑迁移等能力；

## 特性
- **高可靠性：**  
  基于paxos算法实现多副本数据同步，Master节点故障时主从自动切换，在无Master或者Master漂移过程仍可保证锁状态的持续一致性，不影响正常锁操作；
  <br><br>
- **高吞吐：**  
  集群多节点互为主备部署，多paxos分组的Master均匀分布在所有集群节点，不同paxos分组的锁操作并行处理，相同paxos分组锁操作批量合并处理，大大提升了系统的吞吐量；
  <br><br>
- **易用性：**
丰富的锁接口封装，开箱即用；

## 为什么选择WLock
在分布式领域中，分布式锁已经是一种比较成熟的技术，现有的实现方案也有很多，例如基于Redis封装的Redisson Lock、RedLock，以及基于Zookeeper、Etcd等系统的封装实现，既然已经存在这么多分布式锁方案，为什么还开发WLock，优势又有哪些？接下来我们从功能、服务特性、性能三个维度，介绍下这几种分布式锁的差异化。
#### 功能差异
| 实现 |  wlock  |  redis（redisson）|  zookeeper(curator)  |  etcd (jetcd) 
| ------ | ------ | ------ | ------ | ------ | 
| 锁类型| 公平锁/优先级锁/读写锁 | 公平锁/非公平锁/读写锁 | 公平锁/读写锁 | 公平锁 |
| 锁续约 | 支持（自动/主动触发） | 仅支持watchdog自动续约 | 不支持 | 支持 | 
| 异步watch机制 | 支持 | 不支持 | 支持 | 支持 |   
| TTL机制 | 支持 | 支持 | 不支持 | 支持 |   
| 锁粒度 | 进程/线程 | 线程 | 线程 | 进程/线程 |
#### 服务特性
| 实现 |  wlock  |  redis（redisson）| redis （redlock） |  zookeeper(curator)  |  etcd (jetcd)
| ------ | ------ | ------ | ------ | ------ | ------ | 
| 可用性 | N/2 + 1可用 | 主从(异步) | N/2 + 1可用 | N/2 + 1可用 | N/2 + 1可用，但是在leader发生切换期间，不提供服务 |
| 可靠性 | 高 | 低，主从切换数据可能丢失 | 略高，对节点间时钟一致性要求高，单节点挂掉数据不能同步恢复 | 高 | 高 |
| 单客户端并发 | 最高到5.5W左右 | 最高到3W左右 | 最高到2.9W左右 | 最高到6.5K左右 | 最高到5.5K左右 |
| 系统吞吐量 | 13.6万+ | 3万 | 3万 | 8750 | 6793 |
| 响应延迟 | 700微妙+ | 200微妙+ | 600微妙+ | 2毫秒+ | 2毫秒+ |
| 接入复杂度 | 简单，已封装接口 | 简单，已封装接口 | 简单，已封装接口  | 简单，已封装接口（非curator sdk需自行封装） | 需要封装 |

#### 性能

#### 测试运行环境
```
机器配置：
CPU：20 x Intel(R) Xeon(R) Silver 4114 CPU @ 2.20GHz  
内存：192 GB  
硬盘：ssd  
网卡：万兆网卡  
服务端集群机器个数：3台
```

#### 测试结果

**单客户端qps：**

<img src="document/img/qps.png" height="60%" width="60%" />


**相同并发下，请求响应延迟（单位ms）**

<img src="document/img/rt.png" height="60%" width="60%" />

说明：以上对比测试的中数据，redis、zk、etcd相关非官方数据，均由我们在相同环境下实际压测得到。其中，对于qps的统计，客户端请求一次加锁再请求一次释放锁合并为一次计数，更详细的压测数据及压测条件可查看[开源对比](BENCHMARK.md)文档。

通过以上几个维度的测试分析，WLock的优势在于可靠性与系统吞吐量比较高，处理延迟略低于redis，但明显高于zookeeper与etcd，为此，对于分布式锁选型有以下建议:  
1. 对可靠性要求不高，响应延迟比较敏感的场景，锁并发低于3W时可使用redis，高于3W建议用WLock；
2. 对可靠性要求比较高，同时锁并发高于500的场景，可使用WLock；

## 快速使用


#### 本地运行 WLock
WLock 运行在所有主流操作系统上，只需要安装 Java JDK 8 或更高版本。要检查，请运行`java -version`：
```
$ java -version
java version "1.8.0_121"
```

#### 服务初始化步骤 : 
1. **创建数据表** 
	- wlock 注册中心,为方便快速启动,使用 H2 数据库,线上建议使用 mysql,建表请参考 [ SQL](document/sql/create.sql).
2. **部署注册中心并启动** - [详情](document/DEPLOY.md)
3. **创建集群** - [详情](document/DEPLOY.md)
4. **添加节点** - [详情](document/DEPLOY.md)
5. **节点上线** - [详情](document/DEPLOY.md)
6. **服务端初始化** - [详情](document/DEPLOY.md)


#### 客户端初始化

**1. 注册秘钥**

```shell
# 访问注册中心节点通过 swagger 快速注册秘钥
http://localhost:8888/swagger-ui/index.html#/key-rest/addKeyUsingPOST
```

**2. 依赖客户端jar包**

```shell
# 客户端 jar 包位置
cd target/client
```

```xml
<dependency>
  <artifactId>wlock.client</artifactId>
  <groupId>com.wuba.wlock</groupId>
  <version>{project.version}</version>
</dependency>
```
**3. 查看秘钥配置**
```shell
http://localhost:8888/swagger-ui/index.html#/key-rest/getKeyListUsingPOST
```

**4. 初始化**

```java
wLockClient = new WLockClient("test123_8", "127.0.0.1", 22020);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);
```

**参数说明 :**  
**keyHash** ：秘钥名称,从秘钥配置中获取  
**registryIp** ：注册中心 ip  
**registryPort** ：注册中心端口  
**lockKey** ：分布式锁名称  
**WDistributedLock** ：分布式锁对象封装

#### 互斥锁示例

```java
String keyPath = "/opt/wlock.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

AcquireLockResult lockResult = wdLock.tryAcquireLock(30000, 10000, new LockExpireListener() {
	@Override
	public void onExpire(String key) {
		System.out.println("key expired");
	}
});
```


#### 读写锁示例
```java
WLockClient wlockClient= new WLockClient("test123_8", "127.0.0.1", 22020);
WReadWriteLock wReadWriteLock = wlockClient.newReadWriteLock("test_key" );
WReadLock readLock = wReadWriteLock.readLock();
WWriteLock  writeLock = wReadWriteLock.writeLock();
AcquireLockResult readResult = readLock.tryAcquireLock(1000 * 60, 1000 * 60);
AcquireLockResult writeResult = writeLock.tryAcquireLock(1000 * 60, 1000 * 60);
```

**以上只是简单使用demo,详细使用说明请参考[使用文档](document/USE.md)**

## 文档

[开源对比](document/BENCHMARK.md) 
[部署文档](document/DEPLOY.md) 
[接口文档](document/USE.md) 
[分布式锁源码实现对比](document/CONTRAST.md)

## 参考
[How to do distributed locking](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)

## 未来规划
1. 提供go、php等多语言sdk
2. 开源web管控中心、监控模块
3. 支持分布式信号量机制

## 如何贡献
诚挚邀请对分布式锁感兴趣的同学一起参与WLock项目的开发建设，或提出宝贵意见和建议，参与方式可参考文档[CONTRIBUTING](CONTRIBUTING.md)。Star&Fork也是对我们最大的支持。


## 开源许可
WLock项目基于[Apache License 2.0](./LICENSE)协议开源

## 联系我们
<img src="document/img/wlock-wechat.png"/>  
欢迎添加58技术微信账号，加入wlock技术讨论群~
