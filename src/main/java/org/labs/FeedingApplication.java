package org.labs;

import org.labs.services.DeveloperFeedingService;

import java.util.concurrent.atomic.AtomicLong;

public class FeedingApplication {
    public static void main(String[] args) throws InterruptedException {
        DeveloperFeedingService developerFeedingService = new DeveloperFeedingService(7, 2, 1_000_000);
        AtomicLong[] result = developerFeedingService.getBowlsEaten();
        developerFeedingService.startLunch();
        for (int i = 0; i < result.length; i++) {
            System.out.printf("Developer number {%d} eaten {%d}\n", i, result[i].get());
        }
    }
}