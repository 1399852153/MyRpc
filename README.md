# MyRpc
自己动手实现一个简单的rpc框架(觉得有帮助希望能随手点个star)

##### lab1(实现点到点的rpc通信)
1. 网络通信(netty做客户端、服务端网络交互，服务端使用线程池)
2. 支持序列化（实现序列化方式的抽象，支持json、hessian、jdk序列化等）
3. 客户端代理生成(拓展点：先实现jdk代理，后可选用javassist、cglib等字节码增强，减少反射开销)

##### lab2(实现集群间rpc通信)
1. 服务注册 + 注册中心集成(拓展点：先支持本地文件做测试再支持用zookeeper做注册中心，后可选用redis、nacos等)
2. 负载均衡策略(拓展点：先支持roundRobin轮训，后支持随机、权重等)
3. 使用时间轮，支持设置调用超时时间

##### 理论上可以继续拓展的点（参考dubbo等成熟的rpc框架）
1. 服务健康检查
2. 服务版本控制(灰度、支持服务的多版本)
3. 支持重试策略
4. 优雅关闭 优雅启动
5. 与spring集成
6. 以上所有拓展点支持spi
7. 支持熔断、限流
8. 支持admin控制台

#####
* lab1博客：[自己动手实现rpc框架(一) 实现点对点的rpc通信](https://www.cnblogs.com/xiaoxiongcanguan/p/17506728.html)
* lab2博客：[自己动手实现rpc框架(二) 实现集群间rpc通信](https://www.cnblogs.com/xiaoxiongcanguan/p/17533373.html)