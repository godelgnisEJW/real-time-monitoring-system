import java.text.SimpleDateFormat
import java.util
import java.util.{Date, Properties, TimeZone}

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
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


case class LogItem(ts: Long, operatorType: String, num: Long)
case class LogCount(windowEnd: Long, operatorType:String, num:Long){
  def getWindowEnd: Long = windowEnd
  def getOperatorType: String = operatorType
  def getNum: Long = num
}
case class HistoryItem(windowEnd: String, pileUpNum: Long){
  def getWindowEnd: String = windowEnd
  def getPileUpNum: Long = pileUpNum
}

object QueueStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //kafka配置文件
    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.50.139:29092,192.168.50.139:29093,192.168.50.139:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "queue")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("queueLog", new SimpleStringSchema, pros))
        .map(data => {
          val array = data.split("\\t")
          LogItem(array(0).toLong, array(1), array(2).toLong)
        })
        .assignAscendingTimestamps(_.ts)
        .keyBy(_.operatorType)
        .timeWindow(Time.seconds(1L))
        .aggregate(new AggCount, new WindowChanel)
        .keyBy(_.windowEnd)
        .timeWindowAll(Time.seconds(1L))
        .process(new FinalQueueStatistics(10))
    dataStream.print("queueLog").setParallelism(1)
    env.execute("queueLogJob")
  }
}

class AggCount extends AggregateFunction[LogItem, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: LogItem, accumulator: Long): Long = value.num + accumulator

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

class WindowChanel extends WindowFunction[Long, LogCount, String, TimeWindow]{
  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[LogCount]): Unit = {
    out.collect(LogCount(window.getEnd, key, input.iterator.next()))
  }
}

class FinalQueueStatistics(windowSize: Int) extends ProcessAllWindowFunction[LogCount, String, TimeWindow]{
  private var historyState: ListState[HistoryItem] = _
  private var lastState: ValueState[Long] = _
  private var jedis: Jedis = _
  override def open(parameters: Configuration): Unit = {
    historyState = getRuntimeContext.getListState(new ListStateDescriptor[HistoryItem]("history-state", classOf[HistoryItem]))
    lastState = getRuntimeContext.getState(new ValueStateDescriptor[Long]("last-state", classOf[Long]) )
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("192.168.50.139", 6379)
  }

  override def process(context: Context, elements: Iterable[LogCount], out: Collector[String]): Unit = {
    var sum = 0L
    if(Option(lastState.value()).isDefined) sum = lastState.value()
    for (e <- elements){
      if (e.operatorType.equals("send"))
        sum += e.num
      else
        sum -= e.num
    }
    lastState.update(sum)

    val queue = new util.LinkedList[HistoryItem]
    import scala.collection.JavaConverters.iterableAsScalaIterableConverter
    for(item <- historyState.get().asScala){
      queue.addLast(item)
    }
    if (queue.size() >= windowSize) queue.removeFirst()
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    queue.addLast(HistoryItem(dateFormat.format(new Date(context.window.getEnd)), sum))
    historyState.update(queue)

    val jsonStrHistory = JSON.toJSONString(queue.toArray, SerializerFeature.PrettyFormat)
    val transation = jedis.multi()
    val key = "QUEUE:PILEUP:NUM"
    transation.set(key, jsonStrHistory)
    transation.publish(key, jsonStrHistory)
    transation.exec()

    out.collect(jsonStrHistory)
  }
}