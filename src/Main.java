import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class Main {

    // Задача 1: Общий счётчик
    static class Task1 {
        static class Counter {
            private int value = 0;
            public synchronized void increment() { value++; }
            public int getValue() { return value; }
        }

        public static void run() throws InterruptedException {
            Counter counter = new Counter();
            Thread[] threads = new Thread[5];
            for (int i = 0; i < 5; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) counter.increment();
                });
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.println("[Task1] Counter: " + counter.getValue());
        }
    }

    // Задача 2: Потокобезопасный список
    static class Task2 {
        public static void run() throws InterruptedException {
            CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 1; j <= 100; j++) list.add(j);
                });
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.println("[Task2] Size: " + list.size());
        }
    }

    // Задача 3: Пул потоков
    static class Task3 {
        public static void run() {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            for (int i = 0; i < 20; i++) {
                int taskId = i;
                executor.submit(() ->
                        System.out.println("[Task3] " + Thread.currentThread().getName() + " Task " + taskId)
                );
            }
            executor.shutdown();
        }
    }

    // Задача 4: Банковские переводы
    static class Task4 {
        static class Account {
            private int balance;
            public Account(int balance) { this.balance = balance; }
            public void transfer(Account to, int amount) {
                synchronized (Account.class) {
                    if (this.balance >= amount) {
                        this.balance -= amount;
                        to.balance += amount;
                    }
                }
            }
            public int getBalance() { return balance; }
        }

        public static void run() throws InterruptedException {
            Account a = new Account(1000);
            Account b = new Account(1000);
            Thread t1 = new Thread(() -> { for (int i = 0; i < 100; i++) a.transfer(b, 10); });
            Thread t2 = new Thread(() -> { for (int i = 0; i < 100; i++) b.transfer(a, 10); });
            t1.start(); t2.start(); t1.join(); t2.join();
            System.out.println("[Task4] A: " + a.getBalance() + ", B: " + b.getBalance());
        }
    }

    // Задача 5: CyclicBarrier
    static class Task5 {
        public static void run() {
            int threadCount = 5;
            CyclicBarrier barrier = new CyclicBarrier(threadCount, () ->
                    System.out.println("[Task5] Все потоки прошли барьер")
            );
            for (int i = 0; i < threadCount; i++) {
                int finalI = i;
                new Thread(() -> {
                    try {
                        System.out.println("[Task5] Поток " + finalI + " работает");
                        Thread.sleep(1000);
                        barrier.await();
                        System.out.println("[Task5] Поток " + finalI + " завершил фазу");
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        }
    }

    // Задача 6: Semaphore
    static class Task6 {
        public static void run() {
            Semaphore sem = new Semaphore(2);
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    try {
                        sem.acquire();
                        System.out.println("[Task6] Поток " + finalI + " использует ресурс");
                        Thread.sleep(2000);
                        sem.release();
                    } catch (InterruptedException e) { e.printStackTrace(); }
                }).start();
            }
        }
    }

    // Задача 7: Callable и Future
    static class Task7 {
        private static long factorial(int n) {
            if (n == 0) return 1;
            long result = 1;
            for (int i = 1; i <= n; i++) result *= i;
            return result;
        }

        public static void run() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            Future<Long>[] futures = new Future[10];
            for (int i = 0; i < 10; i++) {
                int num = i + 1;
                futures[i] = executor.submit(() -> factorial(num));
            }
            for (Future<Long> f : futures) System.out.println("[Task7] " + f.get());
            executor.shutdown();
        }
    }

    // Задача 8: BlockingQueue
    static class Task8 {
        public static void run() {
            BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
            new Thread(() -> { // Producer
                try {
                    for (int i = 0; i < 10; i++) {
                        queue.put(i);
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();

            new Thread(() -> { // Consumer
                try {
                    while (true) {
                        Integer item = queue.take();
                        System.out.println("[Task8] Обработано: " + item);
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        }
    }

    // Задача 9: Многопоточная сортировка
    static class Task9 {
        private static int[] merge(int[][] chunks) {
            return Arrays.stream(chunks)
                    .flatMapToInt(Arrays::stream)
                    .sorted()
                    .toArray();
        }

        public static void run() throws Exception {
            int[] data = {5, 3, 2, 6, 1, 4, 9, 8, 7};
            int parts = 3;
            ExecutorService executor = Executors.newFixedThreadPool(parts);
            int[][] chunks = new int[parts][];
            Future<?>[] futures = new Future[parts];

            for (int i = 0; i < parts; i++) {
                int start = i * (data.length / parts);
                int end = (i == parts - 1) ? data.length : start + (data.length / parts);
                chunks[i] = Arrays.copyOfRange(data, start, end);
                int chunkIndex = i;
                futures[i] = executor.submit(() -> Arrays.sort(chunks[chunkIndex]));
            }
            for (Future<?> f : futures) f.get();
            executor.shutdown();

            System.out.println("[Task9] " + Arrays.toString(merge(chunks)));
        }
    }

    // Задача 10: Обед философов
    static class Task10 {
        static class Philosopher implements Runnable {
            private final Lock leftFork;
            private final Lock rightFork;
            public Philosopher(Lock left, Lock right) {
                this.leftFork = left;
                this.rightFork = right;
            }
            @Override
            public void run() {
                try {
                    while (true) {
                        leftFork.lock();
                        rightFork.lock();
                        System.out.println(Thread.currentThread().getName() + " ест");
                        Thread.sleep(1000);
                        rightFork.unlock();
                        leftFork.unlock();
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        public static void run() {
            int count = 5;
            Lock[] forks = new ReentrantLock[count];
            for (int i = 0; i < count; i++) forks[i] = new ReentrantLock();
            for (int i = 0; i < count; i++) {
                Lock left = forks[i];
                Lock right = forks[(i + 1) % count];
                new Thread(new Philosopher(left, right), "[Task10] Философ " + (i + 1)).start();
            }
        }
    }

    // Задача 11: Умножение матриц
    static class Task11 {
        public static void run() {
            int[][] a = {{1, 2}, {3, 4}};
            int[][] b = {{5, 6}, {7, 8}};
            int[][] result = new int[a.length][b[0].length];
            ExecutorService executor = Executors.newFixedThreadPool(a.length);

            for (int i = 0; i < a.length; i++) {
                int row = i;
                executor.submit(() -> {
                    for (int j = 0; j < b[0].length; j++) {
                        for (int k = 0; k < a[0].length; k++) {
                            result[row][j] += a[row][k] * b[k][j];
                        }
                    }
                });
            }
            executor.shutdown();
            System.out.println("[Task11] Результат:");
            for (int[] row : result) System.out.println(Arrays.toString(row));
        }
    }

    // Задача 12: Таймер с остановкой
    static class Task12 {
        public static void run() {
            Thread timerThread = new Thread(() -> {
                long start = System.currentTimeMillis();
                while (!Thread.currentThread().isInterrupted()) {
                    long elapsed = (System.currentTimeMillis() - start) / 1000;
                    System.out.println("[Task12] Прошло: " + elapsed + " сек");
                    try { Thread.sleep(1000); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            });

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    timerThread.interrupt();
                    System.out.println("[Task12] Таймер остановлен");
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();

            timerThread.start();
        }
    }

    public static void main(String[] args) throws Exception {
        Task1.run();
        Task2.run();
        Task3.run();
        Task4.run();
        Task5.run();
        Task6.run();
        Task7.run();
        Task8.run();
        Task9.run();
        Task10.run();
        Task11.run();
        Task12.run();
    }
}