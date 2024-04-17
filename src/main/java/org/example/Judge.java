package org.example;

import org.example.data.Answers;
import org.example.data.Exam;
import org.example.reader.AnswerReader;
import org.example.reader.ExamReader;
import org.example.utils.CustomThreadPool;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Judge {
    public final CustomThreadPool threadPool;
    private final String examsPath;
    // 答案文件夹路径
    private final String answersPath;
    // 输出文件路径
    private final String output;
    Judge(String examsPath, String answersPath, String output, CustomThreadPool threadPool){
        this.examsPath = examsPath;
        this.answersPath = answersPath;
        this.output = output;
        this.threadPool = threadPool;
    }
    public void run(){
        Exam[] exams;
        Answers[] answers;
        exams = ExamReader.readDir(examsPath);
        answers = AnswerReader.readDir(answersPath);
        int[][] result = new int[answers.length][3];

        //compile
        List<Future<Boolean>> list = new ArrayList<>();
        try {
            String resourcePath = Paths.get(getClass().getClassLoader().getResource("cases").toURI()).toString();
            String path = resourcePath + "\\answers\\code-answers";
            File folder = new File(path);
            File[] files = folder.listFiles();
            for(File file:files){
                String Path = file.getPath();
                if(Path.endsWith(".java")){
                    Future<Boolean> future = threadPool.submit(()->{
                        Charset charset = Charset.forName("GBK");
                        String command = "javac " + Path;
                        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
                        Process process = processBuilder.start();
                        int exitCode = process.waitFor();
                        return exitCode == 0;
                    });
                    list.add(future);
                }
            }

            //确保编译已全部完成
            for(Future future:list){
                future.get();
            }

        }
        catch (URISyntaxException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }


        for (int i = 0; i < answers.length; i++) {
            result[i][0] = answers[i].getExamId();
            result[i][1] = answers[i].getStuId();
            ExamJudge examJudge = new ExamJudge();
            result[i][2] = examJudge.judge(exams,answers[i],threadPool);
        }
        CsvWriter writer = new CsvWriter();
        writer.write(result, output);
    }
}
