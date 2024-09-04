package com.atguigu.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main..start..");
        /**
         * 实现多线程：
         * 1。继承Thread
         * 2。实现Runnable接口
         * 3。实现Callable接口
         * 4。线程池
         */
        System.out.println("Thread01..start..");
        Thread01 thread01 = new Thread01();
        thread01.start();
        System.out.println("Thread01..end..");

        System.out.println("Runable01..start..");
        Runable01 runable01 = new Runable01();
        new Thread(runable01).start();
        System.out.println("Runable01..end..");

        System.out.println("Callable01..started..");
        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
        new Thread(futureTask).start();
        // （阻塞等待 ）等待线程执行完成，获取返回结果
        Integer result = futureTask.get();
        System.out.println("Callable01..end..");

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // 常见线程池
//        Executors.newCachedThreadPool(); // 核心为0 所有都可以回收
//        Executors.newFixedThreadPool(); // 固定大小
//        Executors.newScheduledThreadPool(); // 定时
//        Executors.newSingleThreadExecutor(); // 单线程


    }

    public static class Thread01 extends Thread{
        public void run(){
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 3;
            System.out.println("运行结果：" + i);
        }
    }
    public static class Runable01 implements Runnable{

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 3;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Callable01 implements Callable<Integer>{

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 3;
            System.out.println("运行结果：" + i);
            return i;
        }
    }


}
