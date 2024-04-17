package org.example;

import org.example.utils.CustomThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CustomThreadPool pool = new CustomThreadPool(5);
        List<Future> list = new ArrayList<>();
        for(int i = 0;i < 1000; i++){
            int finalI = i;
            Future<Integer> future = pool.submit(() -> {
                return finalI+1;
            });
            list.add(future);
        }
        for (Future i:list){
            System.out.println("Result:" + i.get());
        }
        pool.shutdown();
    }
    public static int task(int i){
        return i;
    }
}
