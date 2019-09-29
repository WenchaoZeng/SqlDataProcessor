package com.zwc.sqldataprocessor;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

import javax.swing.*;

import com.zwc.sqldataprocessor.core.SqlFileExecutor;

public class ExecuteWindow {

    JFrame frame = null;
    Consumer<String> logPrinter = null;
    TextArea textArea = null;

    public ExecuteWindow() {
        frame = new JFrame("执行信息");
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(600, 400);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(((screenSize.width - frame.getWidth()) / 2), (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);

        textArea = new TextArea("");
        frame.getContentPane().add(textArea);
        logPrinter = msg -> {
            Log.info(msg);
            String text = textArea.getText() + msg + "\n";
            textArea.setText(text);
            frame.repaint();
        };
    }

    public void focus() {
        frame.setVisible(true);
        frame.requestFocus();
    }

    public void exec(String filePath) {

        textArea.setText("");
        frame.repaint();

        new Thread(() -> {
            try {
                SqlFileExecutor.exec(filePath, logPrinter);
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                logPrinter.accept(sw.toString());
                return;
            }

            MainWindow.instance.focus();
        }).start();
    }
}
