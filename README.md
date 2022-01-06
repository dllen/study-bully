# Fork from (https://github.com/TheRusstler/Bully-Algorithm)

# Bully algorithm (霸道选举算法)

霸道选举算法是一种分布式选举算法，每次都会选出存活的进程中ID最大的候选者。


## 霸道选举算法的选举流程

选举过程中会发送以下三种消息类型：

1. Election(ELECT)消息：表示发起一次选举
2. Answer(OK)消息：对发起选举消息的应答
3. Coordinator(RESULT)消息：选举胜利者向参与者发送选举成功消息

触发选举流程的事件包括：

1. 当进程P从错误中恢复
2. 检测到Leader失败

选举流程：

1. 如果P是最大的ID，直接向所有人发送RESULT消息，成功新的Leader；否则向所有比他大的ID的进程发送Election消息
2. 如果P再发送ELECT消息后没有收到OK消息，则P向所有人发送RESULT消息，成功新的Leader
3. 如果P收到了从比自己ID还要大的进程发来的OK消息，P停止发送任何消息，等待RESULT消息（如果过了一段时间没有等到RESULT消息，重新开始选举流程）
4. 如果P收到了比自己ID小的进程发来的ELECT消息，回复一个OK消息，然后重新开始选举流程
5. 如果P收到RESULT消息，把发送者当做Leader

## 霸道选举算法的假设

霸道选举算法的假设包括：

- 假设了可靠的通道通信，更进一步的假设是系统中任何两个进程之间都可以通信。
- 每个进程都知道其他进程的编号，也就是说算法依赖一个全局的数据。
- 假设进程能够明确地判断出一个正常运行的进程和一个已经崩溃的进程。

## Bully算法缺陷

### Leader假死

Leader节点承担的职责负载过重的情况下，可能无法即时对组内成员作出响应，这种便是假死。 如果 *(N)节点* 假死，于是 *(N-1)节点* 成为了Leader节点，但是在(N)
节点负载减轻之后，(N)节点又对组内成员作出了响应，(N)节点又会成为Leader节点，如此反复，整个集群状态就会非常不可靠。

#### 怎么解决假死问题？

发起选举时候，需要半数以上的节点认定 *(N)节点*  不存活了，才发起选举。

### 脑裂问题

脑裂问题指的是一个集群中出现了两个及以上的Leader节点。 集群因为网络原因分成了两个部分，两个部分因为网络原因不能通讯，各种选出了自己的Leader。

#### 怎么解决脑裂问题？

**Quorum 算法**
成为Leader需要半数以上的节点确认。 如果产生了脑裂情况，为了避免脑裂的Leader生成错误数据对整个集群产生影响。 Leader
更新集群状态时还作出了如下防护，Leader有两种指令，一种是send指令，另一种是commit指令，Leader将最新集群状态推送给其它节点的时候(这是send指令)，
Leader节点进入等待响应状态，其它节点并不会立刻应用该集群状态，而是首先会响应Leader节点表示它已经收到集群状态更新，同时等待Leader节点的commit指令。 Leader 节点如果在
commit_timeout 配置时间内都没有收到 minimum_master_nodes 个数的节点响应，那么Leader节点就不会向其它节点发送commit指令。
如果Leader收到了足够数量的响应，那么Leader会向集群发出提交状态的指令，此时其它节点应用集群最新状态，Leader节点再次等待所有节点响应，等待时间为
publish_timeout，如果任何一个节点没有发出提交响应，Leader再次更新整个集群状态更新。

**Leader 主动降级**

Leader主动降级发生在两种情况。 第一种是Leader发现自己能连接到的其它节点数目小于n/2 + 1，那么Leader自动降级为candidate。
第二种是Leader在ping其它节点时候，如果发现了其它Leader，那么当前的Leader会比较cluster_state的version，如果当前Leader的version小，那么主动降级为Candidate并主动加入另外一个Leader节点

### 网络负载问题
集群中每个节点成员都会维护和其它所有成员的交互，整个集群维护的网络连接的总数是n*(n-1)，如果集群中节点的数目非常的多，那么网络连接数目也会非常的多，网络负载会比较大。
生成环境使用限制并发连接数。

## References
- [Elasticsearch选举原理之Bully算法](https://zhuanlan.zhihu.com/p/110015509)
- [Bully algorithm wikipedia](https://en.wikipedia.org/wiki/Bully_algorithm)
- [Leader Election, Why Should I Care?](https://www.elastic.co/cn/blog/found-leader-election-in-general)
