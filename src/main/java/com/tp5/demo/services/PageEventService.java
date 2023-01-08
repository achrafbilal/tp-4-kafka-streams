package com.tp5.demo.services;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.tp5.demo.entities.PageEvent;

@Service
public class PageEventService {

	//private List<String> users=new ArrayList<>();

	private List<String> devices=new ArrayList<>();

	public PageEventService(){
		for (int i = 1; i <= 20; i++) {
			//users.add(String.format("User %d",i));
			devices.add(String.format("Device %d",i));
		}
	}

	@Bean
	public Consumer<PageEvent> pageEventConsumer(){
		return (input)->{
			System.out.println("********");
			System.out.println(input.toString());
			System.out.println("********");
		};
	}

	@Bean
	public Supplier<PageEvent> pageEventSupplier(){
		Random random=new Random();
		return ()-> new PageEvent(
				Math.random()>0.5?"P1":"P2",
				devices.get(random.nextInt(devices.size()-1)),
				new Date(),
				new Random().nextInt(9000)
		) ;
	}

	@Bean
	public Function<PageEvent,PageEvent> pageEventFunction(){
		return (input)->{
			input.setName("Page Event");
			return input;
		};
	}

	@Bean
	public Function<KStream<String,PageEvent>,KStream<String,Long>> kStreamFunction(){
		return (input)-> {
			KStream<String,Long> result=input
					.filter((k,v)->v.getDuration()>100)
					.map((k,v)->new KeyValue<>(v.getName(),0L))
					.groupBy((k,v)->k,Grouped.with(Serdes.String(),Serdes.Long()))
					.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
					.count(Materialized.as("page-count"))
					.toStream()
					.map(
						(k,v)->{
							KeyValue <String,Long> kv=new KeyValue<>(
								k.window().startTime()+" => "+k.window().endTime()+" "+k.key(),
								v
							);
							System.out.println(kv.key+" "+kv.value);
							return kv;
						}
					);
			return  result;
		};
	}
}
