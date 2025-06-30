package com.tmd.ai.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RunPythonWithConda {

    public void face(String[] args) {
        // conda环境名
        String condaEnv = "pruc";
        // python脚本路径
        String pythonScript = "C:\\Users\\wwwsh\\IdeaProjects\\agent-ai\\src\\face\\resources\\static\\facial-expression\\face.py";


        // 组装命令
        List<String> commands = new ArrayList<>();
        commands.add("conda");
        commands.add("run");
        commands.add("-n");
        commands.add(condaEnv);
        commands.add("python");
        commands.add(pythonScript);
        // 如果有参数，可以继续添加，比如 commands.add("arg1");

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.directory(new File("src/face/resources/static/facial-expression"));
            pb.redirectErrorStream(true); // 合并输出和错误流
            Process process = pb.start();

            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code " + exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
