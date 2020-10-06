package scau.godelgnis.realtimebackend.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class RedisMessageWarp {
    private String topic;
    private String message;
}
