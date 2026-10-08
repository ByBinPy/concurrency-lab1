    package org.labs.services;

    import java.time.Duration;
    import java.util.Arrays;
    import java.util.concurrent.CountDownLatch;
    import java.util.concurrent.atomic.AtomicBoolean;
    import java.util.concurrent.atomic.AtomicInteger;
    import java.util.concurrent.atomic.AtomicLong;

    public class DeveloperFeedingService {

        private static final boolean FREE_SPOON = false;
        private static final boolean BUSY_SPOON = true;


        private final AtomicBoolean[] spoons;
        private final AtomicInteger[] waiters;
        private final AtomicLong[] bowlsEaten;
        private final Thread[] developers;
        private final AtomicLong bowlCount;

        public DeveloperFeedingService(int developerCount, int waitersCount, long bowlCount) {
            this.spoons = initSpoons(developerCount);
            this.developers = new Thread[developerCount];
            this.bowlCount = new AtomicLong(bowlCount);
            this.bowlsEaten = initBowlsByDev(developerCount);
            this.waiters = initWaitersRestrictions(developerCount, waitersCount);
        }

        public AtomicLong[] getBowlsEaten() {
            return bowlsEaten;
        }

        public void startLunch() throws InterruptedException {
            CountDownLatch starter = new CountDownLatch(1);
            for (int i = 0; i < developers.length; i++) {
                int developerPosition = i;
                developers[i] = new Thread(() -> {
                    try {
                        starter.await();
                        System.out.println("Started process");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    while (true) {
                        askWaiter(developerPosition);
                        int left = getLeftSpoon(developerPosition);
                        int right = getRightSpoon(developerPosition);
                        acquireSpoon(left);
                        acquireSpoon(right);
                        boolean ate = tryEat(developerPosition);
                        releaseSpoon(left);
                        releaseSpoon(right);
                        getTipsForWaiterAndSendRespect(developerPosition);
                        if (!ate) {
                            return;
                        }
                    }
                });
                developers[i].start();
            }

            Thread checkThread = new Thread(() -> {
                while (true) {
                    if (sumEatenBowls() == 1_000_000) {
                        System.out.println("Process ended");
                        return;
                    }
                    for (int i = 0; i < bowlsEaten.length; i++) {
                        System.out.printf("Developer {%d}, eaten: {%d}. ", i, bowlsEaten[i].get());
                    }
                    System.out.println();
                    try {
                        Thread.sleep(Duration.ofSeconds(2));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            Thread checkThread1 = new Thread(() -> {
                while (true) {
                    if (sumEatenBowls() == 1_000_000) {
                        System.out.println("Process ended");
                        return;
                    }
                    for (int i = 0; i < spoons.length; i++) {
                        System.out.printf("spoon {%d}, has status: {%s}. ", i, spoons[i].get());
                    }
                    System.out.println();
                    try {
                        Thread.sleep(Duration.ofSeconds(2));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            checkThread.start();
            checkThread1.start();
            starter.countDown();

            for (Thread developer : developers) {
                developer.join();
            }
        }

        private void releaseSpoon(int spoonNumber) {
            AtomicBoolean spoon = spoons[spoonNumber];
            spoon.setRelease(FREE_SPOON);
        }

        private void getTipsForWaiterAndSendRespect(int developerPosition) {
            int waiterNumber = developerPosition % waiters.length;
            waiters[waiterNumber].incrementAndGet();
        }

        private void askWaiter(int developerPosition) {
            int waiterNumber = developerPosition % waiters.length;
            AtomicInteger waiter = waiters[waiterNumber];
            while (true) {
                var waiterFreePositions = waiter.get();
                if (waiterFreePositions > 0 && waiter.compareAndSet(waiterFreePositions, waiterFreePositions - 1)) {
                    return;
                }
            }
        }

        private void acquireSpoon(int spoonNumber) {
            AtomicBoolean spoon = spoons[spoonNumber];
            while (true) {
                boolean spoonStatus = spoon.get();
                if (spoonStatus == FREE_SPOON && spoon.compareAndSet(FREE_SPOON, BUSY_SPOON)) {
                    return;
                }
            }
        }

        private boolean tryEat(int developerPosition) {
            while (true) {
                long currentBowlCount = bowlCount.get();
                if (currentBowlCount <= 0) {
                    return false;
                } else {
                    if (bowlCount.compareAndSet(currentBowlCount, currentBowlCount - 1)) {
                        AtomicLong bowlsEatenByDeveloper = bowlsEaten[developerPosition];
                        while (true) {
                            long eatenByDeveloper = bowlsEatenByDeveloper.get();
                            if (bowlsEatenByDeveloper.compareAndSet(eatenByDeveloper, eatenByDeveloper + 1)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        private int getLeftSpoon(int developerPosition) {
            return developerPosition > 0 ? developerPosition - 1 : spoons.length - 1;
        }

        private int getRightSpoon(int developerPosition) {
            return developerPosition < spoons.length - 1 ? developerPosition + 1 : 0;
        }

        private static AtomicBoolean[] initSpoons(int spoonsCount) {
            AtomicBoolean[] spoons = new AtomicBoolean[spoonsCount];
            for (int i = 0; i < spoonsCount; i++) {
                spoons[i] = new AtomicBoolean(false);
            }
            return spoons;
        }

        private static AtomicLong[] initBowlsByDev(int developerCount) {
            AtomicLong[] bowlsByDev = new AtomicLong[developerCount];
            for (int i = 0; i < developerCount; i++) {
                bowlsByDev[i] = new AtomicLong(0);
            }

            return bowlsByDev;
        }

        private static AtomicInteger[] initWaitersRestrictions(int developerCount, int waitersCount) {
            int defaultWaitersThreshold = Math.floorDiv(developerCount, waitersCount);
            int lastWaiterThreshold = developerCount - (waitersCount - 1) * defaultWaitersThreshold - 1;
            AtomicInteger[] waiters = new AtomicInteger[waitersCount];
            if (isWaiterThresholdValid(defaultWaitersThreshold) && isWaiterThresholdValid(lastWaiterThreshold)) {
                Arrays.fill(waiters, 0, waitersCount - 1, new AtomicInteger(defaultWaitersThreshold));
                waiters[waitersCount - 1] = new AtomicInteger(lastWaiterThreshold);
            } else {
                throw new IllegalArgumentException("Cannot organize work with {%d} developers and {%d} waiters".formatted(developerCount, waitersCount));
            }

            return waiters;
        }

        private static boolean isWaiterThresholdValid(int waiterThreshold) {
            return waiterThreshold >= 2;
        }

        private Long sumEatenBowls() {
            return Arrays.stream(bowlsEaten).map(AtomicLong::get).reduce(Long::sum).orElse(0L);
        }
    }
