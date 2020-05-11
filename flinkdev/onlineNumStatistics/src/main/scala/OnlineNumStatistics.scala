import java.text.SimpleDateFormat
import java.util.{Date, Properties, TimeZone}

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerConfig
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

case class LogItem(ts: Long, num: Int, logType: String)

class ResultCount(windowEnd: String, num: Long){
  def getWindowEnd: String = windowEnd
  def getNum: Long = num
}

object OnlineNumStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    //    设置环境时间语义
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "119.23.172.151:29092,119.23.172.151:29093,119.23.172.151:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "online")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    //    读取数据
    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("log-in-out-log", new SimpleStringSchema(), pros))
      .map(data => {
        val array = data.split("\\t")
        LogItem(array(0).toLong, array(1).toInt, array(2))
      })
      .assignAscendingTimestamps(_.ts)
      .timeWindowAll(Time.seconds(1))
      .process(new MyFunction(10))

    dataStream.print("log-in-out-log")
    env.execute("onlineNum")
  }
}

class MyFunction(listSize: Int) extends ProcessAllWindowFunction[LogItem, String, TimeWindow]{


  var lastOnlineNum: ValueState[Long] = _
  var resultList: ListState[ResultCount] = _
  var jedis: Jedis = _

  override def open(parameters: Configuration): Unit = {
    lastOnlineNum = getRuntimeContext.getState(new ValueStateDescriptor[Long] ("last-online-num",classOf[Long]))
    resultList = getRuntimeContext.getListState(new ListStateDescriptor[ResultCount]("result-num", classOf[ResultCount]))
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("119.23.172.151", 6379)
  }

  override def process(context: Context, elements: Iterable[LogItem], out: Collector[String]): Unit = {
    //    val start = context.window.getStart
    val end = context.window.getEnd
    val lastNum = lastOnlineNum.value()
    var sum = if (Option(lastNum).isEmpty) 0L else  lastNum

    for(item <- elements){
      if(item.logType.equals("login"))
        sum += item.num
      else
        sum -= item.num
    }
    //    更新当前的在线人数
    lastOnlineNum.update(sum)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    val thisWindowResult = new ResultCount(dateFormat.format(new Date(end)), sum)
    val queue = new ListBuffer[ResultCount]
    //    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters.{bufferAsJavaList, iterableAsScalaIterable}
    for(item <- iterableAsScalaIterable(resultList.get())) queue += item
    //    判断队列中的数据是否达到上限
    if(queue.size >= listSize) queue.remove(0)
    queue += thisWindowResult
    //    更新当前的队列数据
    resultList.update(bufferAsJavaList(queue))
    //    val result = new StringBuilder
    //    result.append("开始时间：").append(new Timestamp(start)).append("\n")
    //        .append("结束时间：").append(new Timestamp(end)).append("\n")
    //        .append("在线人数：").append(sum)

    //    将每次的窗口聚合结果发布到redis
    val jsonStr = JSON.toJSONString(queue.toArray, SerializerFeature.PrettyFormat)
    val transaction = jedis.multi()
    transaction.set("ONLINE:NUM", jsonStr)
    transaction.publish("ONLINE:NUM", jsonStr)
    transaction.exec()


    out.collect(jsonStr)

  }
}


class AggCount extends AggregateFunction[LogItem, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: LogItem, accumulator: Long): Long = {
    if(value.logType == "login")
      accumulator + value.num
    else
      accumulator - value.num
  }

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

//class WindowResult extends WindowFunction[Long, ResultCount, String, TimeWindow]{
//  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[ResultCount]): Unit = {
//    out.collect(ResultCount(key, window.getEnd, input.iterator.next()))
//  }
//}
//
//
//class MyKedProcessFunction extends KeyedProcessFunction[Long, ResultCount, String]{
//  var lastOnlineNum: ValueState[Long] = _
//  var resultList: ListState[ResultCount] = _
//
//  override def open(parameters: Configuration): Unit = {
//    lastOnlineNum = getRuntimeContext.getState(new ValueStateDescriptor[Long] ("last-online-num",classOf[Long]))
//    resultList = getRuntimeContext.getListState(new ListStateDescriptor[ResultCount]("result-num", classOf[ResultCount]))
//  }
//
//  override def processElement(value: ResultCount, ctx: KeyedProcessFunction[Long, ResultCount, String]#Context, out: Collector[String]): Unit = {
//    resultList.add(value)
//    ctx.timerService().registerEventTimeTimer(value.windowEnd)
//  }
//
//  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, ResultCount, String]#OnTimerContext, out: Collector[String]): Unit = {
//    var sum =lastOnlineNum.value()
//    if(sum == null){
//      sum = 0L
//    }
//    import scala.collection.JavaConversions._
//    for(i <- resultList.get()){
//        sum += i.num
//    }
//    lastOnlineNum.update(sum)
//    val result = new StringBuilder
//    result.append("时间：").append(new Timestamp(timestamp - 1000)).append("\n")
//      .append("在线人数：").append(sum).append("\n")
//      .append("===========================")
//    out.collect(result.toString())
//  }
//}
