package com.DTMK.Online.Bookkeeping.Website.Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. {@code @EnableScheduling} activates Spring's built-in cron
 * infrastructure, which the {@link com.DTMK.Online.Bookkeeping.Website.Project.config.RecurringBillScheduler}
 * uses to auto-post recurring bills every day at 00:05 Asia/Shanghai.
 */
@EnableScheduling
@SpringBootApplication
public class OnlineBookkeepingWebsiteProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineBookkeepingWebsiteProjectApplication.class, args);
	}

}
