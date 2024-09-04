package com.atguigu.gulimall.search.thread;

import java.sql.PreparedStatement;
import java.util.concurrent.*;

public class CompletableTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main..start..");
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 3;
//            System.out.println("运行结果：" + i);
//        });

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).whenComplete((t,u)->{
//            System.out.println("异步任务成功..结果是" + t + "异常是" + u); // 感知器
//        }).exceptionally(throwable -> {
//            return 10; // 感知异常，同时返回默认值
//        });

        // handle 方法执行完后的处理
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).handle((res,thr)->{
//            if(res!=null){
//                return res*2;
//            }
//            if(thr!=null){
//                return 0;
//            }
//            return 0;
//        });
        /**
         * thenRunAsync 不能接收上一步的结果， 无返回值
         * thenAcceptAsync 能接收上一步的结果， 无返回值
         * thenApplyAsync 能接收上一步的结果 ， 有返回值
         */
//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenRunAsync(()->{
//            System.out.println("task2 starting !!!");
//        });

//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenAcceptAsync(res->{
//            System.out.println("task2 starting !!" + res);
//        },executor);

//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println("task2 starting !!!");
//            return "hello" + res;
//        });
//        String s = future.get();
//        System.out.println(s);

        /**
         * 两个都完成
         */
        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1开始：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("任务1结束：" + i);
            return i;
        }, executor);
        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2开始：" + Thread.currentThread().getId());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("任务2结束：");
            return "hello";
        }, executor);
        // 两个都完成之后执行
//        future01.runAfterBothAsync(future02,()->{
//            System.out.println("任务3开始");
//        },executor);

//        future01.thenAcceptBothAsync(future02,(f1,f2)->{
//            System.out.println("任务3开始" + f1 + " " + f2);
//        },executor);

//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            return f1 + " " + f2 + "hello";
//        }, executor);
//        String s = future.get();
//        System.out.println(s);

        /**
         * 两个任务只要有一个完成， 就执行
         *  runAfterEitherAsync 不感知结果，自己也无返回值
         */
//        future01.runAfterEitherAsync(future02,()->{
//            System.out.println("任务三开始");
//        },executor);

//        future01.acceptEitherAsync(future02,(res)->{
//            System.out.println("任务三开始" + res);
//        },executor);

        CompletableFuture<String> future = future01.applyToEitherAsync(future02, (res) -> {
            return res + " hello ";
        }, executor);
        String s = future.get();
        System.out.println(s);
        System.out.println("main..end..");
    }
}
