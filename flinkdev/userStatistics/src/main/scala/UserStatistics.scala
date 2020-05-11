import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Date, Properties, TimeZone}

import com.alibaba.fastjson.serializer.SerializerFeature
import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor, ValueState, ValueStateDescriptor}
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


case class UserItem(ts: Long, source: String, num: Long, proportion: Double)
case class UserViewCount(windowsEnd: Long, source: String, num: Long, active: Long)
class ResultViewCount(var source: String, var num: Long, var userSourceProportion: String, var active: Long, var activeProportion: String){
  def getSource: String = source
  def getNum: Long = num
  def getUserSourceProportion: String = userSourceProportion
  def getActive: Long = active
  def getActiveProportion: String = activeProportion
}

object UserStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //kafka配置文件
    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "119.23.172.151:29092,119.23.172.151:29093,119.23.172.151:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "user")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("userLog", new SimpleStringSchema, pros))
        .map(data => {
          val array = data.split("\\t")
          UserItem(array(0).toLong, array(1), array(2).toLong, array(3).toDouble)
        })
        .assignAscendingTimestamps(_.ts)
        .keyBy(_.source)
        .timeWindow(Time.seconds(1L))
        .aggregate(new AggCount, new WindowChanel)
        .keyBy(_.windowsEnd)
        .timeWindowAll(Time.seconds(1L))
        .process(new FinalUserStatistics)
    dataStream.print("userLog").setParallelism(1)
    env.execute("userLogJob")
  }
}
class AggCount extends AggregateFunction[UserItem, (Long, Long), (Long, Long)]{
  override def createAccumulator(): (Long, Long) = (0L, 0L)

  override def add(value: UserItem, accumulator: (Long, Long)): (Long, Long) = (accumulator._1 + value.num, accumulator._2 + (value.num * value.proportion).toLong)

  override def getResult(accumulator: (Long, Long)): (Long, Long) = accumulator

  override def merge(a: (Long, Long), b: (Long, Long)): (Long, Long) = (a._1 + b._1, a._2 + b._2)
}

class WindowChanel extends WindowFunction[(Long, Long), UserViewCount, String, TimeWindow]{
  override def apply(key: String, window: TimeWindow, input: Iterable[(Long, Long)], out: Collector[UserViewCount]): Unit = {
    val element = input.iterator.next()
    out.collect(UserViewCount(window.getEnd, key, element._1, element._2))
  }
}

class FinalUserStatistics extends ProcessAllWindowFunction[UserViewCount, String, TimeWindow]{
  private var jedis: Jedis = _
  private var historyState: MapState[String, ResultViewCount] = _
  private var lastSum: ValueState[Long] = _
  override def open(parameters: Configuration): Unit = {
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("119.23.172.151", 6379)
    historyState = getRuntimeContext.getMapState(new MapStateDescriptor[String, ResultViewCount]("history-state", classOf[String], classOf[ResultViewCount]))
    lastSum = getRuntimeContext.getState(new ValueStateDescriptor[Long]("last-sum", classOf[Long]))
  }

  override def process(context: Context, elements: Iterable[UserViewCount], out: Collector[String]): Unit = {
    var sum = 0L    //累计总用户数
    if (Option(lastSum.value()).isDefined) sum = lastSum.value()    //判断是否有历史数据
//      存在历史数据，取得历史值，加上当前值并更新数据，否则，当前值更新到状态中
//      更新各端的累计用户数
    for (e <- elements){
      val lastOpt = Option(historyState.get(e.source))
      if (lastOpt.isDefined){
        val lastResult = historyState.get(e.source)
        val allNum = lastResult.num + e.num
        val allActive = lastResult.active + e.active
        lastResult.num = allNum
        lastResult.active = allActive
        sum += e.num
      }else{
        historyState.put(e.source, new ResultViewCount(e.source, e.num, "", e.active, ""))
        sum += e.num
      }
    }
    lastSum.update(sum)   //更新总用户数
    //更新各端用户占比数据
    import scala.collection.JavaConverters.iterableAsScalaIterableConverter
    val listBuffer = new ListBuffer[ResultViewCount]
    for (key <- historyState.keys().asScala) {
      val resultItem = historyState.get(key)
      val userSourceProportion  = resultItem.num.toDouble / sum * 100
      val activeProportion = resultItem.active.toDouble / resultItem.num * 100
      resultItem.userSourceProportion = String.format("%s%%",BigDecimal.apply(userSourceProportion).setScale(2, BigDecimal.RoundingMode.HALF_UP).toString())
      resultItem.activeProportion = String.format("%s%%",BigDecimal.apply(activeProportion).setScale(2, BigDecimal.RoundingMode.HALF_UP).toString())
      listBuffer += resultItem
    }
//    序列化数据
    val jsonObj = new JSONObject()
    jsonObj.put("userSourceData", listBuffer.toArray)
    jsonObj.put("allUserNum", sum)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    jsonObj.put("windowsEnd", dateFormat.format(new Date(context.window.getEnd)))
//    错误写法
//    jsonObj.put("windowsEnd", new Timestamp(context.window.getEnd).toString)
    val jsonStrUserData = JSON.toJSONString(jsonObj, SerializerFeature.PrettyFormat)

//    更新redis缓存数据
    val trasation = jedis.multi()
    val key = "USER:SOURCES:PROPORTION"
    trasation.set(key, jsonStrUserData)
    trasation.publish(key, jsonStrUserData)
    trasation.exec()
    out.collect(jsonStrUserData)
  }
}