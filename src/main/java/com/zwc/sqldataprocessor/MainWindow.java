package com.zwc.sqldataprocessor;

import java.awt.*;

import javax.swing.*;

public class MainWindow {

    public static void main(String[] args) throws Exception {

        Log.info("Start main window. App version: %s", Global.version);

        // 窗口
        JFrame frame = new JFrame("屏幕分享");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 100);
        frame.setLocation(700, 400);

        // 文本框
        TextField textField = new TextField("启动中");
        frame.getContentPane().add(textField);

        frame.setVisible(true);
    }

}
