package org.labs.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeveloperFeedingServiceTest {
    @Test
    public void testRightServiceConstructionResult() {
        int developersCount = 10;
        int waitersCount = 2;
        long bowlCount = 100;
        assertDoesNotThrow(() -> new DeveloperFeedingService(developersCount, waitersCount, bowlCount));
        DeveloperFeedingService feedingService = new DeveloperFeedingService(developersCount, waitersCount, bowlCount);
        assertEquals(developersCount, feedingService.getBowlsEaten().length);
    }
    @Test
    public void testServiceConstructionResult_withInvalidConstructParams() {
        int developersCount = 10;
        int waitersCount = 5;
        long bowlCount = 100;
        assertThrows(IllegalArgumentException.class, () -> new DeveloperFeedingService(developersCount, waitersCount, bowlCount));
    }
}
