package com.zwc.sqldataprocessor;

import java.awt.*;

import javax.swing.*;

public class ExecuteWindow {
    public ExecuteWindow(String filePath) {
        String shortName = filePath.substring(filePath.lastIndexOf("/") + 1);
        JFrame frame = new JFrame(shortName);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);

        TextArea textArea = new TextArea("");
        frame.getContentPane().add(textArea);

        new Thread(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String text = textArea.getText() + "test\n";
            textArea.setText(text);
            frame.repaint();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            frame.dispose();
        }).start();
    }
}
