基于消息驱动的开发几乎成了微服务架构下必备开发方式之一。这是因为，第一原来传统单体架构开发中的接口调用开发已经在微服务架构下不存在；；第二微服务架构的开发要求降低各微服务直接的依赖耦合，一旦我们在某个微服务中直接调用另外一个微服务，那么这两个微服务就会通过依赖产生了强耦合；第三微服务的自治原则也强烈要求各微服务之间不能够互相调用。因此，在微服务架构开发中基于消息驱动的开发成为了一种必然趋势。

让我们来看一下示例工程中的一个场景：

* USER-CONSUMER要求能够实现自治，尽量降低对商品微服务的依赖。
* USER-CONSUMER为了能够保障服务的效率，开发小组决定对USER-SERVICE数据进行缓存，这样只需要第一次加载的时候远程调用USER-SERVICE微服务，当用户下次再请求USER-SERVICE的时候就可以从缓存中获取，从而提升了服务效率（至少使用内存方式还是redis来实现缓存，这个由你决定）。

如果按照上面的场景进行实现，在大部分情况下系统都可以稳定工作，一旦USER-SERVICE进行修改那该怎么办，我们总不至于在USER-SERVICE微服务中再去调用USER-CONSUMER微服务吧，这样岂不是耦合的更紧密了。嗯，是的，这个时候就可以让消息出动了。

通过引入消息，我们示例工程的系统架构将变为下图所示：

![](https://upload-images.jianshu.io/upload_images/1488771-14af22e0b8dad91f.png?imageMogr2/auto-orient/)

基于上面这个架构图我们看一下基于消息如何来实现。之前如果你使用过消息中间件应该对开发基于消息应用的难度心有戚戚然，不过当我们使用Spring Cloud时，已经为我们的开发提供了一套非常不错的组件 -- Stream。

> 实现消息驱动开发

接下来的改造将分为下面三步：

* 安装Kafka服务器;
* 改造USER-SERVICE微服务，实现商品消息的发送;
* 改造USER-CONSUMER微服务，实现商品消息的监听。

> 安装Kafka服务器

我们接下来的示例会使用Kafka作为消息中间件，一方面是Kafka消息中间件非常轻便和高效，另外一方面自己非常喜欢使用`Kafka`中间件。如果你不想使用Kafka那么可以自行完成于`RabbitMQ`的对接，而具体实现的业务代码则不需要进行任何改动。

```
brew install kafka
安装会依赖zookeeper。 
注意：安装目录：/usr/local/Cellar/kafka/0.10.2.0
```
安装的配置文件位置。

```
/usr/local/etc/kafka/server.properties
/usr/local/etc/kafka/zookeeper.properties
```
启动zookeeper

`zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties &`

启动kafka

`kafka-server-start /usr/local/etc/kafka/server.properties &`

创建topic

让我们使用单个分区和只有一个副本创建一个名为“test”的主题

`kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test`

查看topics

`kafka-topics --list --zookeeper localhost:2181`

发送一些消息

Kafka提供了一个命令行客户端，它将从文件或标准输入接收输入，并将其作为消息发送到Kafka集群。默认情况下，每行都将作为单独的消息发送。
运行生产者，然后在控制台中键入一些消息发送到服务器。

`kafka-console-producer --broker-list localhost:9092 --topic test`

消费消息

Kafka还有一个命令行消费者，将消息转储到标准输出。

`kafka-console-consumer --bootstrap-server localhost:9092 --topic test --from-beginning`


> Stream原理浅析

从Spring Cloud Stream核心原理上来说，Stream提供了一个与消息中间件进行消息收发的抽象层，这个也是Spring所擅长的。通过该抽象层剥离了业务中消息收发与实际所使用中间件直接的耦合，从而使得我们可以轻松与各种消息中间件对接，也可以很简单的就可以实现所使用的消息中间件的更换。这点和我们使用ORM框架一样，可以平滑的在多种数据库之间进行切换。

Stream应用模型

从这个应用开发上来说，Stream提供了下述模型：

![](https://upload-images.jianshu.io/upload_images/1488771-37660c923095a734.png?imageMogr2/auto-orient/)


在该模型图上有如下几个核心概念:

* Source: 当需要发送消息时，我们就需要通过Source，Source将会把我们所要发送的消息(POJO对象)进行序列化（默认转换成JSON格式字符串），然后将这些数据发送到Channel中；
* Sink: 当我们需要监听消息时就需要通过Sink来，Sink负责从消息通道中获取消息，并将消息反序列化成消息对象(POJO对象)，然后交给具体的消息监听处理进行业务处理；
* Channel: 消息通道是Stream的抽象之一。通常我们向消息中间件发送消息或者监听消息时需要指定主题（Topic）／消息队列名称，但这样一旦我们需要变更主题名称的时候需要修改消息发送或者消息监听的代码，但是通过Channel抽象，我们的业务代码只需要对Channel就可以了，具体这个Channel对应的是那个主题，就可以在配置文件中来指定，这样当主题变更的时候我们就不用对代码做任何修改，从而实现了与具体消息中间件的解耦；
* Binder: Stream中另外一个抽象层。通过不同的Binder可以实现与不同消息中间件的整合，比如上面的示例我们所使用的就是针对Kafka的Binder，通过Binder提供统一的消息收发接口，从而使得我们可以根据实际需要部署不同的消息中间件，或者根据实际生产中所部署的消息中间件来调整我们的配置。

Stream应用原理

从上面我们了解了Stream的应用模型，消息发送逻辑及流程我们也清晰了。那么我们在实际消息发送和监听时又是怎么操作的呢？

在使用上Stream提供了下面三个注解:

* @Input: 创建一个消息输入通道，用于消息监听;
* @Output: 创建一个消息输出通道，用于消息发送;
* @EnableBinding: 建立与消息通道的绑定。

我们在使用时可以通过@Input和@Output创建多个通道，使用这两个注解创建通道非常简单，你只需要将他们分别注解到接口的相应方法上即可，而不需要具体来实现该注解。当启动Stream框架时，就会根据这两个注解通过动态代码生成技术生成相应的实现，并注入到Spring应用上下文中，这样我们就可以在代码中直接使用。

Output注解

对于@Output注解来说，所注解的方法的返回值必须是MessageChannel，MessageChannel也就是具体消息发送的通道。比如下面的代码：

```
public interface ProductSource {
    @Output
    MessageChannel hotProducts();

    @Output
    MessageChannel selectedProducts();
}
```

这样，我们就可以通过ProductSource所创建的消息通道来发送消息了。

Input注解

对于@Input注解来说，所注解的方法的返回值必须是SubscribableChannel，SubscribableChannel也就是消息监听的通道。比如下面的代码：

```
public interface ProductSink {
    @Input
    SubscribableChannel productOrders();
}
```
这样，我们就可以通过ProductSink所创建的消息通道来监听消息了。

关于Input、Output的开箱即用
 
或许你有点迷糊，之前我们在代码中使用了Source、Sink，那么这两个类和上面的注解什么关系呢？让我们来看一下这两个接口的源码：
 
```
// Source源码
public interface Source {

  String OUTPUT = "output";

  @Output(Source.OUTPUT)
  MessageChannel output();

}

// Sink源码
public interface Sink {

  String INPUT = "input";

  @Input(Sink.INPUT)
  SubscribableChannel input();

}
```
是不是有点恍然大悟呀，@Input和@Output是Stream核心应用的注解，而Source和Sink只不过是Stream为我们所提供开箱即用的两个接口而已，有没有这两个接口我们都可以正常使用Stream。

此外，Stream还提供了一个开箱即用的接口Processor，源码如下：

```
public interface Processor extends Source, Sink {
}
```

也就是说Processor只不过是同时可以作为消息发送和消息监听，这种接口在我们开发消息管道类型应用时会非常有用。

自定义消息通道名称
 
前面，我们讲了消息通道是Stream的一个抽象，通过该抽象可以避免与消息中间件具体的主题耦合，那么到底是怎么一回事呢？从Source和Sink源码中可以看到，所注解的@Output和@Input注解中都有一个参数，分别为output和input，这个时候你再观察一下我们之前的配置：

```
# 商品微服务中的配置
spring.cloud.stream.bindings.output.destination=twostepsfromjava-cloud-producttopic
spring.cloud.stream.bindings.output.content-type=application/json

# Mall-Web中的配置
spring.cloud.stream.bindings.input.destination=twostepsfromjava-cloud-producttopic
spring.cloud.stream.bindings.input.content-type=application/json
spring.cloud.stream.bindings.input.group=mallWebGroup
```

从配置中可以看到destination属性的配置，分别指定了output和inout也就是Stream中所使用的消息通道名称。因此，我们可以通过这两个注解来分别设置消息通道的名称，比如：

```
public interface ProductProcessor {
    @Output("pmsoutput")
    MessageChannel productOutput();

    @Input("pmsinput")
    SubscribableChannel input();}
```

这样，当我们使用ProductProcessor接口来实现消息发送和监听的时就需要在配置文件中配置如下：

```
# 消息发送
spring.cloud.stream.bindings.pmsoutput.destination=twostepsfromjava-cloud-producttopic
spring.cloud.stream.bindings.pmsoutput.content-type=application/json

# 消息监听
spring.cloud.stream.bindings.pmsinput.destination=twostepsfromjava-cloud-producttopic
spring.cloud.stream.bindings.pmsinput.content-type=application/json
spring.cloud.stream.bindings.pmsinput.group=mallWebGroup
```

绑定

既然，消息发送通道和监听通道都创建好了，那么将它们对接到具体的消息中间件就可以完成消息的发送和监听功能了，而@EnableBinding注解就是用来实现该功能。具体使用方式如下：

```
// 实现发送的绑定
@EnableBinding(Source.class)
public class Application {

}

// 实现监听的绑定
@EnableBinding(Sink.class)
public class ProductMsgListener {

}
```

需要说明的是，@EnableBinding可以同时绑定多个接口，如下：

`@EnableBinding(value={ProductSource.class, ProductSink.class})`

直接使用通道
 
前面我们消息发送的代码如下：

```			
protected void sendMsg(String msgAction, String itemCode) {
    ProductMsg productMsg = new ProductMsg(msgAction, itemCode);
    this.logger.debug("发送商品消息:{} ", productMsg);
    
    // 发送消息
    this.source.output().send(MessageBuilder.withPayload(productMsg).build());
}
```
获取你在想既然@Output所提供的MessageChannel才是最终消息发送时使用的，那么我们是否可以直接使用呢？的确这个是可以的，上面的代码我们可以更改成如下： 

```
@Service
public class ProductService {
    protected Logger logger = LoggerFactory.getLogger(ProductService.class);

    private MessageChannel output;
    private List<ProductDto> productList;

    @Autowired
    public ProductService(MessageChannel output) {
        this.output = output;
        this.productList = this.buildProducts();
    }
    
     // 省略了其它代码
     
    /**
     * 具体消息发送的实现
     * @param msgAction 消息类型
     * @param itemCode 商品货号
     */
    protected void sendMsg(String msgAction, String itemCode) {
        ProductMsg productMsg = new ProductMsg(msgAction, itemCode);
        this.logger.debug("发送商品消息:{} ", productMsg);

        // 发送消息
        this.output.send(MessageBuilder.withPayload(productMsg).build());
    }
}
```
默认Stream所创建的MessageChannelBean的Id为方法名称，但是如果我们在@Output注解中增加了名称定义，如果：

```
public interface ProductSource {
    @Output("pmsoutput")
    MessageChannel output();
}
```
那么这个时候Stream会使用pmsoutput作为Bean的Id，而我们的代码也需要为如下：

```
@Autowired
public ProductService(@Qualifier("pmsoutput") MessageChannel output) {
    this.output = output;
    this.productList = this.buildProducts();
}
```
。

具体代码看demo






