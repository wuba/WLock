## 锁种类

#### 互斥锁
```java
WLockClient wlockClient= new WLockClient("test123_8", "127.0.0.1", 22020);
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);
```

#### 读写锁
```java
WLockClient wlockClient= new WLockClient("test123_8", "127.0.0.1", 22020);
WReadWriteLock wReadWriteLock = wlockClient.newReadWriteLock("test_key");
WReadLock readLock = wReadWriteLock.readLock();
WWriteLock  writeLock = wReadWriteLock.writeLock();
```

#### 进程锁
```java
WLockClient wlockClient= new WLockClient("test123_8", "127.0.0.1", 22020);
// 进程锁 : 同一个进程下的所有线程共用同一个锁
WDistributedLock wdLock = wlockClient.newDistributeLock(lock, LockPolicy.Process);
```

<span style='color:red;'>注意：同一个进程，建议wlockClient实例只初始化一次，不同锁操作可以复用，否则与服务端创建过多连接，消耗过多内存。</span>

## 锁持有者LockOwner

由以下三个属性定义：

**ownerHost**：锁持有者主机host

**ownerPID**：锁持有者进程ID

**ownerThreadID**：锁持有者线程ID



## 服务端返回给客户端的状态码说明

**ResponseStatus. SUCCESS** : 请求成功

**ResponseStatus. TIMEOUT** ：请求超时

**ResponseStatus. ERROR** ：请求异常（有可能为协议解析异常）

**ResponseStatus. LOCK_OCCUPIED** ：锁已经被占用

**ResponseStatus. LOCK_DELETED** ：锁已经被删除

**ResponseStatus. LOCK_CHANGED_OWNER** ：锁owner发生变更

**ResponseStatus. TOKEN_ERROR** ：锁版本号发生变化

 

其中**SUCCESS**、**TIMEOUT**、**ERROR**三种状态码是所有接口调用都可能返回的，

<span style='color:red;'> 若状态码为ResponseStatus.TIMEOUT，可能是网络问题，建议业务重试；</span>

<span style='color:red;'> 若状态码为ResponseStatus.ERROR，可能为协议解析异常，致命，测试时出现该返回码需联系管理员确认客户端使用版本；</span>



## 竞争锁

> 该接口互斥锁和读写锁通用

### 基本说明

#### 1.竞争锁接口有以下几个配置选项：

**waitAcquire**：是否阻塞等待获取到锁，true为阻塞，false为非阻塞

**expireTime**：锁过期时间，单位毫秒，默认值为5分钟，最大取值5分钟，最小值5秒

**maxWaitTime**：最长等待获取锁的时间，单位毫秒，最大值Long.MAX_VALUE 

**weight**：锁权重，默认都为1，取值范围[1, 10]，权重越高，获取到锁概率越高

**renewInterval**：自动续约间隔，单位毫秒(默认为Integer.MAX_VALUE，不自动续租，最小自动续租间隔为1000ms,最大自动续租间隔不能超过过期时间，由业务控制)。

**renewListener**：续约Listener回调

**lockExpireListener**：锁过期Listener回调

**watchListener**：异步监听事件回调

**holdLockListener** : 长期持有锁回调 ,用于持有锁超过 5min 的场景,持有锁超过 5min 必须设置回调

#### 2.竞争锁返回结果AcquireLockResult说明

**ret**：竞争锁结果，true为成功，false为失败

**responseStatus**：结果状态码

**lockVersion**：当前锁的版本号

**LockOwner**：当前锁的持有者

### 同步阻塞方式竞争锁

<table border="同步阻塞方式竞争锁">
	<tr>
        <td colspan="2">1.同步阻塞方式竞争锁</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口(多种参数组合，省略的参数取默认值)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime, int renewInterval)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime, LockExpireListener lockExpireListener)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime, int weight, LockExpireListener lockExpireListener)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime , int maxWaitTime, int weight, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener)</td>
    </tr>
     <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(int expireTime, HoldLockListener holdLockListener)</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
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

**说明：**

所谓同步阻塞竞争锁，是指客户端除非达到最大等待时间maxWaitTime仍然没有获取到锁，或者客户端向服务端连续发送三次请求都超时，或者协议解析异常，其它情况都会阻塞直到获取到锁。

### 同步非阻塞方式竞争锁

<table border="同步非阻塞方式竞争锁">
    <tr>
        <td colspan="2">2.同步非阻塞方式竞争锁</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLockUnblocked(int expireTime)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLockUnblocked(int expireTime, int renewInterval)</td>
    </tr>
     <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLockUnblocked(int expireTime, HoldLockListener holdLockListener)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLockUnblocked(int expireTime, LockExpireListener lockExpireListener)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLockUnblocked(int expireTime, int renewInterval, RenewListener renewListener, LockExpireListener lockExpireListener)</td>
    </tr>
</table>



**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

AcquireLockResult lockResult = wdLock.tryAcquireLockUnblocked(30000, new LockExpireListener() {
		@Override
		public void onExpire(String key) {
			System.out.println("key expired");
		}
});
```

**说明：**

所谓同步非阻塞竞争锁，是指客户端竞争锁的时候，若抢占失败，不会阻塞，立即向客户端返回结果。

同步非阻塞竞争锁接口服务端返回状态码除了SUCCESS、ERROR、TIMEOUT，还有ResponseStatus. LOCK_OCCUPIED状态码，表示锁已经被占用，获取锁失败；

### 竞争锁(自定义选项)

<table border="竞争锁(自定义选项)">
    <tr>
        <td colspan="2">竞争锁(自定义选项)</td>
    </tr>
    <tr>
        <td>AcquireLockResult</td>
        <td>tryAcquireLock(LockOption lockOption)</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

LockOption lockOption = LockOption.newOption().setWaitAcquire(true).setExpireTime(30000).setMaxWaitTime(10000).setRenewInterval(10000).setLockExpireListener(new LockExpireListener(){
		@Override
		public void onExpire(String lockkey) {
			System.out.println("key expired");
		}});
AcquireLockResult lockResult = wdLock.tryAcquireLock(lockOption);
```

**说明:**

该接口整合同步阻塞与同步非阻塞两种接口实现，所有参数可自定义。

<span style='color:red;'>**竞争锁使用须知【重要】:**</span>

根据客户端对共享资源正常访问时间合理设置锁的过期时间，如果设置太短，锁就有可能在客户端完成对共享资源访问之前过期，从而失去保护；如果设置太长的话，一旦某个持有锁的客户端释放锁失败，就会导致一段时间内其它客户端都无法获到取到锁，从而无法正常工作；

使用自动续约机制，需要在秘钥申请时，打开自动续约开关，否则客户端自动续约设置不生效。若业务启动自动续约机制，建议续约周期为锁有效期的1/3（最小为1秒）。业务在加锁处理逻辑的上层一定添加try catch 异常获，在finally逻辑中释放锁，否则业务线程异常退出时，锁自动续约还会一直执行，导致锁永远不过期，出现死锁。

若锁释放失败和锁过期回调中都自定义了共享资源访问的事务回滚逻辑，需要保证幂等性，避免重复回滚。



## 释放锁
> 该接口互斥锁和读写锁通用

为了保证释放锁的安全性，每个客户端只能释放当前线程持有的锁，若在释放锁之前，锁owner或者锁版本号(fencing token)已发生变化，则释放锁失败。

### 接口摘要

<table border="释放锁">
    <tr>
        <td colspan="2">释放锁</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>releaseLock()</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

LockResult lockResult = wdLock.releaseLock(xxxxxx);
```

**说明:**

lockversion参数省略时，lockversion信息从客户端本地上下文中获取（线程安全）。

释放锁接口服务端返回状态码除了**SUCCESS**、**TIMEOUT**、**ERROR**，还有以下几种类型：

**ResponseStatus. LOCK_DELETED** : 锁已经被删除 , 此时锁的释放结果为失败

**ResponseStatus. LOCK_CHANGED_OWNER** : 锁owner发生变更

**ResponseStatus. TOKEN_ERROR** : 锁版本号发生变化

返回这三种状态码说明锁已经过期或者owner发生变更，释放锁失败，为了保证数据操作的原子性、一致性，需要回滚之前加锁过程对共享资源的访问操作。



## 锁续约

> 该接口互斥锁和读写锁通用


锁续约请求可延长锁的过期时间，从当前时间累加上expireTime作为新的过期时间点。为了保证锁续约的安全性，每个客户端只能对当前线程持有的锁续约，若锁owner或者锁版本号(fencing token)已发生变化，则锁续约失败。前面在介绍竞争锁时已提到过自动续约机制，下面接口主要用于业务主动触发续约。

### 接口摘要

<table border="锁续约">
    <tr>
        <td colspan="2">锁续约</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>renewLock(int expireTime)</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

LockResult lockResult = wdLock.renewLock(xxxxxx);
```

**说明:**

lockversion参数省略时，lockversion信息从客户端本地上下文中获取（线程安全）。

锁续约接口服务端返回状态码除了**SUCCESS**、**TIMEOUT**、**ERROR**，还有以下几种类型：

**ResponseStatus. LOCK_DELETED** ：锁已经被删除

**ResponseStatus. LOCK_CHANGED_OWNER** ：锁owner发生变更

**ResponseStatus. TOKEN_ERROR** 锁版本号发生变化

返回这三种状态码说明锁已经过期或者owner发生变更，续约锁失败，应停止续约。

## 异步watch监听

> 该接口互斥锁支持 ,读写锁不支持

客户端可以向服务端注册watch事件，异步监听锁状态变更，当锁状态变化时，服务端立即向客户端推送锁变更事件，客户端通过业务注册的WatchListener 回调处理不同的锁变更事件。同时，还提供了异步竞争锁的机制，在监听到锁释放时，可立即抢占锁。

### 异步监听

<table border="异步监听">
    <tr>
        <td colspan="2">1.异步监听</td>
    </tr>
    <tr>
        <td colspan="2">只监听锁事件变更，不会有抢占锁操作</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    <tr>
    <tr>
        <td>LockResult</td>
        <td>watchlock(long lockversion, WatchListener watchListener)</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>watchlock(long lockversion, long maxWaitTime, WatchListener watchListener)</td>
	</tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

GetLockResult getLockResult = wdLock.getLockState();
long lockversion = -1;
if (getLockResult.isSuccess()) {
	 lockversion = getLockResult.getLockVersion();
}

LockResult lockResult = wdLock.watchlock(xxx, new WatchListener() {
	@Override
	public void onTimeout(String lockkey) {
		// TODO Auto-generated method stub
	}
							
	@Override
	public void onLockReleased(String lockkey) {
		// TODO Auto-generated method stub
	}
							
	@Override
	public void onLockChange(String lockkey, long lockversion) {
		// TODO Auto-generated method stub
	}
							
	@Override
	public void onLockAcquired(String lockkey) {
		// TODO Auto-generated method stub
	}
});
```

**参数说明:**

**lockversion** : 监听锁状态的起始版本，锁更新的版本号大于lockversion时，服务端发送事件通知（只向客户端发送最新锁状态，若有覆盖，不补发中间状态）；

**maxWaitTime** : 最长监听时间，默认为Long.MAX_VALUE，持续监听直到变化。

### 异步监听 + 抢占锁

<table border="异步监听 + 抢占锁">
    <tr>
        <td colspan="2">异步监听 + 抢占锁</td>
    </tr>
    <tr>
        <td colspan="2">锁在服务端被释放时，直接抢占锁</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>watchAndWaitLock(long lockversion, int expireTime, WatchListener watchListener)</td>
    </tr>
   <tr>
        <td>LockResult</td>
        <td>watchAndWaitLock(long lockversion, int expireTime, int renewInterval, WatchListener watchListener)</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>watchAndWaitLock(long lockversion, int expireTime, int renewInterval, int weight, WatchListener watchListener)</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>watchAndWaitLock(long lockversion, int expireTime, int renewInterval, int weight, long maxWaitTime, WatchListener watchListener)</td>
    </tr>
</table>



**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

GetLockResult getLockResult = wdLock.getLockState();
long lockversion = -1;
if (getLockResult.isSuccess()) {
	 lockversion = getLockResult.getLockVersion();
}

AcquireLockResult lockResult = wdLock. watchAndWaitLock (lockversion, 30000,new WatchListener() {......});
```

**参数说明:**

**lockversion** : 监听锁状态的起始版本，锁更新的版本号大于lockversion时，服务端发送事件通知（只向客户端发送最新锁状态，若有覆盖，不补发中间状态）；

**maxWaitTime** : 最长监听时间，默认为Long.MAX_VALUE，持续监听直到变化。

其它参数使用同竞争锁接口。

### 连续 watch 接口

<table border="连续 watch + 抢占锁">
    <tr>
        <td colspan="2">连续 watch + 抢占锁</td>
    </tr>
    <tr>
        <td colspan="2">锁在服务端被释放时，直接抢占锁, 锁异常情况被其他人获取,可以再次抢占</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>continueWatchAndWaitLock(long lockVersion, int weight, long maxWaitTime, WatchListener watchListener)</td>
    </tr>
  <tr>
        <td>LockResult</td>
        <td>continueWatchAndWaitLock(long lockVersion, LockOption lockOption)</td>
    </tr>
</table>



**Demo:**

```java
WDistributedLock wdLock = wLockClient.newDistributeLock(lock);
wdLock.continueWatchAndWaitLock(-1, 1, 2000, new WatchListener() {
    @Override
    public void onLockChange(String lockkey, long lockversion) {
    }

    @Override
    public void onLockReleased(String lockkey) {
    }

    @Override
    public void onLockAcquired(String lockkey) {
    }

    @Override
    public void onTimeout(String lockkey) {
    }
});
```

**参数说明:**

**lockversion** : 监听锁状态的起始版本，锁更新的版本号大于lockversion时，服务端发送事件通知（只向客户端发送最新锁状态，若有覆盖，不补发中间状态）；

**weight** :  锁权重

**maxWaitTime** : 最长监听时间，默认为Long.MAX_VALUE，持续监听直到变化。

其它参数使用同竞争锁接口。




### 异步监听（自定义选项）

<table border="异步监听（自定义选项）">
    <tr>
        <td colspan="2">异步监听（自定义选项）</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>LockResult</td>
        <td>watchlock(long lockversion, LockOption lockOption)</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

GetLockResult getLockResult = wdLock.getLockState();
long lockversion = -1;
if (getLockResult.isSuccess()) {
	 lockversion = getLockResult.getLockVersion();
}

LockOption lockOption = LockOption.newOption().setWaitAcquire(true)
.setExpireTime(30000).setMaxWaitTime(10000).setRenewInterval(10000).setLockExpireListener(new LockExpireListener(){......});
lockOption.setWatchListener(new WatchListener() {......});
LockResult lockResult = wdLock. watchlock(lockOption);
```

**参数说明:**

该接口整合异步监听与异步监听+抢占锁两种接口实现，所有参数可自定义。




## 获取锁状态

### 接口摘要

<table border="获取锁状态">
    <tr>
        <td colspan="2">获取锁状态</td>
    </tr>
    <tr>
        <td>返回结果</td>
        <td>接口</td>
    </tr>
    <tr>
        <td>GetLockResult</td>
        <td>getLockState()</td>
    </tr>
</table>


**Demo:**

```java
String keyPath = "/opt/wlock_test.key";
WLockClient wlockClient = new WLockClient(keyPath);
String lockKey = "my_test_lock";
WDistributedLock wdLock = wlockClient.newDistributeLock(lockKey);

GetLockResult lockResult = wdLock.getLockState();
```

