package com.yuchen.kafkastream;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.TopologyBuilder;

import java.util.Properties;

/**
 * @author yuchen
 */
public class Application {
    public static void main(String[] args) {
        String input = "kafkastream";
        String output = "recommender";

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"logProcesser");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"yuchen:9092");

        //创建KafkaStream的配置
        StreamsConfig config = new StreamsConfig(properties);

        //建立Kafka处理拓扑
        TopologyBuilder builder = new TopologyBuilder();
        builder.addSource("source",input)
                .addProcessor("process",()->new LogProcessor(),"source")
                .addSink("sink",output,"process");

        KafkaStreams kafkaStreams = new KafkaStreams(builder,config);
        kafkaStreams.start();

    }
}
