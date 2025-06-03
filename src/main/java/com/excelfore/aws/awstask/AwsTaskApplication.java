package com.excelfore.aws.awstask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class AwsTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsTaskApplication.class, args);
		log.info("Hi There, Its Working");
	}

}
