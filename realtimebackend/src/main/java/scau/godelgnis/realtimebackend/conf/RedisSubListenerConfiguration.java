package scau.godelgnis.realtimebackend.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import scau.godelgnis.realtimebackend.comment.RedisReceiver;

@Configuration
public class RedisSubListenerConfiguration {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory,
                                            MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("TEST"));
        container.addMessageListener(listenerAdapter, new PatternTopic("ONLINE:NUM"));
        container.addMessageListener(listenerAdapter, new PatternTopic("AREA:HOT"));
        container.addMessageListener(listenerAdapter, new PatternTopic("AREA:RANGE"));
        container.addMessageListener(listenerAdapter, new PatternTopic("INTERFACE:NUM"));
        container.addMessageListener(listenerAdapter, new PatternTopic("QUEUE:PILEUP:NUM"));
        container.addMessageListener(listenerAdapter, new PatternTopic("MSG:NUM"));
        container.addMessageListener(listenerAdapter, new PatternTopic("FILE:SIZE"));
        container.addMessageListener(listenerAdapter, new PatternTopic("USER:SOURCES:PROPORTION"));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisReceiver redisReceiver){
        return new MessageListenerAdapter(redisReceiver, "receiveMessage");
    }
}
