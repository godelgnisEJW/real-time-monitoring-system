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

case class MsgItem(ts: Long, tp: String, num: Long)
case class MsgCount(windowEnd: Long, tp: String, num: Long){
  def getWindowEnd: Long = windowEnd
  def getTp: String = tp
  def getNum: Long = num
}
case class HistoryItem(windowEnd: String, statistics: Array[MsgCount]){
  def getWindowEnd: String = windowEnd
  def getJsonResult: Array[MsgCount] = statistics
}
object MsgStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//        pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.50.139:29092,192.168.50.139:29093,192.168.50.139:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "msg")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("msgLog", new SimpleStringSchema, pros))
      .map(data => {
        val array = data.split("\\t")
        if(array.length == 3)
          MsgItem(array(0).toLong, array(1), array(2).toLong)
        else
          MsgItem(array(0).toLong, array(1), array(2).toLong * array(3).toLong)
      })
      .assignAscendingTimestamps(_.ts)
      .keyBy(_.tp)
      .timeWindow(Time.seconds(1L))
      .aggregate(new AggCount, new WindowChanel)
      .keyBy(_.windowEnd)
      .timeWindowAll(Time.seconds(1L))
      .process(new MsgFinalStatistics(10))

    dataStream.print("msgLog").setParallelism(1)
    env.execute("msgLogJob")
  }
}

class AggCount extends AggregateFunction[MsgItem, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: MsgItem, accumulator: Long): Long = value.num + accumulator

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

class WindowChanel extends WindowFunction[Long, MsgCount, String, TimeWindow]{
  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[MsgCount]): Unit = {
    out.collect(MsgCount(window.getEnd, key, input.iterator.next()))
  }
}

class MsgFinalStatistics(windowSize: Int) extends ProcessAllWindowFunction[MsgCount, String, TimeWindow]{
  var jedis: Jedis = _
  var msgHistoryState: ListState[HistoryItem] = _
  var fileHistoryState: ListState[HistoryItem] = _
  override def open(parameters: Configuration): Unit = {
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("192.168.50.139", 6379)
    msgHistoryState = getRuntimeContext.getListState(new ListStateDescriptor[HistoryItem]("msg-history-state", classOf[HistoryItem]))
    fileHistoryState = getRuntimeContext.getListState(new ListStateDescriptor[HistoryItem]("file-history-state", classOf[HistoryItem]))
  }

  override def process(context: Context, elements: Iterable[MsgCount], out: Collector[String]): Unit = {
    val buffer = new ListBuffer[MsgCount]
    for (i <- elements){
      buffer += i
    }
    val msgBuffer = buffer.filter(_.tp.startsWith("m"))
    val fileBuffer = buffer.filter(_.tp.startsWith("f"))
    process("MSG:NUM", msgHistoryState, context.window.getEnd,msgBuffer, out)
    process("FILE:SIZE", fileHistoryState, context.window.getEnd,fileBuffer, out)
  }

  private def process(key: String, historyState: ListState[HistoryItem], windowEnd: Long, elementsBuffer: ListBuffer[MsgCount], out: Collector[String]): Unit = {

//    获取历史数据
    val queue = new util.LinkedList[HistoryItem]()
    import scala.collection.JavaConverters.iterableAsScalaIterableConverter
    for(i <- historyState.get().asScala){
      queue.addLast(i)
    }
//    历史数据超过显示窗口的长度
    if(queue.size() >= windowSize) queue.removeFirst()
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    queue.addLast(HistoryItem(dateFormat.format(new Date(windowEnd)), elementsBuffer.toArray.sortBy(_.tp)))
//    更新历史数据
    historyState.update(queue)
//    更新redis缓存数据
    val transation = jedis.multi()
    val jsonStrHistory = JSON.toJSONString(queue.toArray, SerializerFeature.PrettyFormat)
    transation.set(key, jsonStrHistory)
    transation.publish(key, jsonStrHistory)
    transation.exec()
//    收集数据
    out.collect(jsonStrHistory)
  }
}