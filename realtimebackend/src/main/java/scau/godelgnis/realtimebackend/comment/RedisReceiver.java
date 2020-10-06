package scau.godelgnis.realtimebackend.comment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scau.godelgnis.realtimebackend.model.RedisMessageWarp;

import java.io.IOException;

@Component
public class RedisReceiver {

    @Autowired
    RedisMessageWarp warp;

    public void receiveMessage(String message, String topic) throws IOException {
        System.out.println("received message: " + message + ", " + topic);
        warp.setMessage(message);
        warp.setTopic(topic);
        WebSocketServer.snedToAll(JSON.toJSONString(warp, SerializerFeature.PrettyFormat));
    }
}
