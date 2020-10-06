package scau.godelgnis.realtimebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import scau.godelgnis.realtimebackend.model.RedisMessageWarp;
import scau.godelgnis.realtimebackend.service.RealTimeDataService;

@RestController
public class RedisValueController {
    @Autowired
    RealTimeDataService realTimeDataService;

    @GetMapping("/getValue/{key}")
    public RedisMessageWarp getRedisValue(@PathVariable  String key){
        return realTimeDataService.getRedisValueByKey(key);
    }
}
