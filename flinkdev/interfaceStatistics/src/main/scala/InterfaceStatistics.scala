import java.text.SimpleDateFormat
import java.util
import java.util.{Date, Properties, TimeZone}

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.{ProcessAllWindowFunction, WindowFunction}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerConfig
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

case class InterfaceItem(ts:Long, serverId: String, tp: String, num: Long)
case class InterfaceCount(windowEnd:Long, serverId: String, num: Long){
  def getWindowEnd: Long = windowEnd
  def getServerId: String = serverId
  def getNum: Long = num
}
case class HistoryItem(ts: String, statistics: Array[InterfaceCount]){
  def getTs: String = ts
  def getJsonResult: Array[InterfaceCount] = statistics
}

object InterfaceStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.50.139:29092,192.168.50.139:29093,192.168.50.139:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "interface")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("interfaceLog", new SimpleStringSchema, pros))
      .map(data => {
        val array = data.split("\\t")
        InterfaceItem(array(0).toLong, array(1), array(2), array(3).toLong)
      })
      .assignAscendingTimestamps(_.ts)

    val processStream = dataStream.keyBy(_.serverId)
      .timeWindow(Time.seconds(1L))
      .aggregate(new AggCount, new WindChanel)
      .keyBy(_.windowEnd)
      .timeWindowAll(Time.seconds(1L))
      .process(new FinalStatistics(10))

    processStream.print("interfaceLog").setParallelism(1)
    env.execute("interfaceLogJob")
  }
}

class AggCount extends AggregateFunction[InterfaceItem, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: InterfaceItem, accumulator: Long): Long = accumulator + value.num

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

class WindChanel extends WindowFunction[Long, InterfaceCount, String, TimeWindow]{

  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[InterfaceCount]): Unit = {
    out.collect(InterfaceCount(window.getEnd, key, input.iterator.next()))
  }
}

class FinalStatistics(historySize: Int) extends ProcessAllWindowFunction[InterfaceCount, String, TimeWindow]{
  private var historyState:ListState[HistoryItem] = _
  private var jedis: Jedis = _
  override def open(parameters: Configuration): Unit = {
    historyState = getRuntimeContext.getListState(new ListStateDescriptor[HistoryItem]("result-list", classOf[HistoryItem]))
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("192.168.50.139", 6379)
  }


  override def process(context: Context, elements: Iterable[InterfaceCount], out: Collector[String]): Unit = {
//    统计当前窗口的数据
    val buffer = new ListBuffer[InterfaceCount]
    for (i <- elements){
      buffer += i
    }
////    序列化数据结果
//    val jsonStrCurrent = JSON.toJSONString(buffer.toArray, SerializerFeature.PrettyFormat)
//    更行历史数据
    val queue = new util.LinkedList[HistoryItem]()
    import scala.collection.JavaConverters.iterableAsScalaIterable
    for (i <- iterableAsScalaIterable(historyState.get())){
      queue.addLast(i)
    }
    if(queue.size() >= historySize) queue.removeFirst()
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    queue.addLast(HistoryItem(dateFormat.format(new Date(context.window.getEnd)), buffer.toArray.sortBy(_.serverId)))
    historyState.update(queue)
//    序列化历史数据
    val jsonStrhistory = JSON.toJSONString(queue.toArray, SerializerFeature.PrettyFormat)
//    更新redis缓存
    val transation = jedis.multi()
    transation.set("INTERFACE:NUM", jsonStrhistory)
    transation.publish("INTERFACE:NUM", jsonStrhistory)
    transation.exec()
    out.collect(jsonStrhistory)
  }

}