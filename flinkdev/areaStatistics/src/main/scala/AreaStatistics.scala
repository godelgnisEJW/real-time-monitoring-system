
import java.sql.Timestamp
import java.util.Properties

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.{ProcessAllWindowFunction, WindowFunction}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerConfig
import redis.clients.jedis.{Jedis, Transaction}

import scala.collection.mutable.ListBuffer

case class AreaItem(ts: Long, num: Long, region: String)
case class MidCount(region:String, windowEnd: Long, num: Long)

class ResultItem(region:String, num: Long){
  def getRegion: String = region
  def getNum: Long = num
}
object AreaStatistics {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val pros = new Properties()
    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092")
//    pros.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.50.139:29092,192.168.50.139:29093,192.168.50.139:29094")
    pros.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "area")
    pros.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    pros.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    pros.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val dataStream = env.addSource(new FlinkKafkaConsumer[String]("areaLog", new SimpleStringSchema(), pros))
      .map(data => {
        val array = data.split("\\t")
        AreaItem(array(0).toLong, array(1).toInt, array(2))
      })
      .assignAscendingTimestamps(_.ts)
      .keyBy(_.region)
      .timeWindow(Time.seconds(1L))
      .aggregate(new AggCount, new WindowEndFunc)
      .keyBy(_.windowEnd)
      .timeWindowAll(Time.seconds(1L))
      .process(new MapProcessor(10))

    dataStream.print("areaLog")
    env.execute("areaStatisticsJob")
  }
}
class AggCount extends AggregateFunction[AreaItem, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: AreaItem, accumulator: Long): Long = accumulator + value.num

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

class WindowEndFunc extends WindowFunction[Long, MidCount, String, TimeWindow]{
  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[MidCount]): Unit = {
    out.collect(MidCount(key, window.getEnd, input.iterator.next()))
  }
}

class MapProcessor(topSize: Int) extends ProcessAllWindowFunction[MidCount, String, TimeWindow]{
  private var areaMap: MapState[String, Long] = _
  private var jedis: Jedis = _
  override def open(parameters: Configuration): Unit = {
    jedis = new Jedis("redis-server", 6379)
//    jedis = new Jedis("192.168.50.139", 6379)
    areaMap = getRuntimeContext.getMapState(new MapStateDescriptor[String, Long] ("area-map", classOf[String], classOf[Long]))
  }

  override def process(context: Context, elements: Iterable[MidCount], out: Collector[String]): Unit = {
    val list = new ListBuffer[ResultItem]
    for(i <- elements){
//      累计区域热度并更新状态
      var hot = i.num
      if(Option(areaMap.get(i.region)).isDefined)
        hot += areaMap.get(i.region)
      areaMap.put(i.region, hot)
    }
//    获取最新的区域热度数据
    val iterator = areaMap.entries().iterator()
    while(iterator.hasNext){
      val item = iterator.next()
      list += new ResultItem(item.getKey, item.getValue)
    }
//    序列化区域热度数据
    val jsonStrAreaHot = JSON.toJSONString(list.toArray, SerializerFeature.PrettyFormat)
//    更新并发布redis区域热度数据
    val hotTransaction:Transaction = jedis.multi()
    hotTransaction.set("AREA:HOT", jsonStrAreaHot)
    hotTransaction.publish("AREA:HOT", jsonStrAreaHot)
    hotTransaction.exec()
//    根据区域热度进行降序排序
    val sortedList = list.sortBy(_.getNum)(Ordering.Long.reverse).take(topSize)
//    序列化区域热度排行
    val jsonStrHotRange = JSON.toJSONString(sortedList.toArray, SerializerFeature.PrettyFormat)
//    更新并发布排行数据
    val rangeTransaction = jedis.multi()
    rangeTransaction.set("AREA:RANGE", jsonStrHotRange)
    rangeTransaction.publish("AREA:RANGE",jsonStrHotRange)
    rangeTransaction.exec()

    val result = new StringBuilder
    result.append("时间：\n").append(new Timestamp(elements.iterator.next().windowEnd)).append("\n")
        .append("区域热度数据：\n").append(jsonStrAreaHot).append("\n")
        .append("区域热度排行：\n").append(jsonStrHotRange)
    out.collect(result.toString)
  }
}