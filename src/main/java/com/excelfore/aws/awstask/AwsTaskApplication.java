package com.excelfore.aws.awstask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.logging.LoggingApplicationListener;

@SpringBootApplication
@Slf4j
public class AwsTaskApplication {

	private static final Logger log = LoggerFactory.getLogger(AwsTaskApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AwsTaskApplication.class, args);
		log.info("Working");
		System.out.println("Go For It");
	}

}
