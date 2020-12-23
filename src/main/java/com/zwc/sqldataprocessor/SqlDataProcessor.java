package com.zwc.sqldataprocessor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import com.zwc.sqldataprocessor.core.DatabaseConfigLoader;

public class SqlDataProcessor {

    JFrame frame;
    int selectedIndex = 0;
    List<Component> fileListComponents = new ArrayList<>();

    ExecuteWindow executeWindow = null;

    public static SqlDataProcessor instance;
    public static void main(String[] args) throws Exception {
        instance = new SqlDataProcessor();
        instance.start();
    }

    public void focus() {
        frame.setVisible(true);
        frame.requestFocus();
    }

    void start() throws Exception {
        Log.info("Start main window. App version: %s", Global.version);

        // 初始化DB配置
        DatabaseConfigLoader.initializeDefaultConfig();

        // 窗口
        frame = new JFrame("SQLDataProcessor");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setLayout(null);
        frame.setVisible(true);

        int leftX = 0;
        int rightX = frame.getWidth();

        // 输入框
        String note = "填入SQL文件完整路径";
        TextField textField = new TextField(note);
        textField.setBounds(leftX + 10, 13, 400, 34);
        textField.setEnabled(true);
        leftX = textField.getX() + textField.getWidth();
        frame.getContentPane().add(textField);

        // 添加文件
        Button openBtn = new Button("添加");
        openBtn.setSize(100, 40);
        openBtn.setLocation(leftX + 5, 10);
        frame.getContentPane().add(openBtn);
        leftX += openBtn.getX() + openBtn.getWidth();
        openBtn.addActionListener(e -> {
            if (textField.getText().equals(note)) {
                return;
            }
            Global.addFile(textField.getText());
            renderFileList();
        });

        // 数据源配置
        Button dbSettingBtn = new Button("配置数据库");
        dbSettingBtn.setSize(90, 40);
        dbSettingBtn.setLocation(rightX - dbSettingBtn.getWidth() - 10, 10);
        frame.getContentPane().add(dbSettingBtn);
        rightX = dbSettingBtn.getX();
        dbSettingBtn.addActionListener(e -> {
            if (e.getID() == ActionEvent.ACTION_PERFORMED) {
                Global.openFile(DatabaseConfigLoader.path);
            }
        });

        // 查看日志
        Button logBtn = new Button("查看日志");
        logBtn.setSize(80, 40);
        logBtn.setLocation(rightX - logBtn.getWidth() - 5, 10);
        frame.getContentPane().add(logBtn);
        rightX = logBtn.getX();
        logBtn.addActionListener(e -> {
            if (e.getID() == ActionEvent.ACTION_PERFORMED) {
                Global.openFile(Log.path);
            }
        });

        renderFileList();
        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (selectedIndex != 0) {
                    selectedIndex = 0;
                    renderFileList();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {}
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
                if (!frame.isFocused()) {
                    return false;
                }
                if (!(ke.getID() == KeyEvent.KEY_PRESSED)) {
                    return false;
                }

                if (ke.getKeyCode() == KeyEvent.VK_UP) {
                    selectedIndex--;
                    if (selectedIndex < 0) {
                        selectedIndex = 0;
                    }
                    renderFileList();
                } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectedIndex++;
                    if (selectedIndex >= Global.fileList.size()) {
                        selectedIndex = Global.fileList.size() -1;
                    }
                    renderFileList();
                } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    startExecute();
                }

                return false;
            }
        });
    }

    void startExecute() {
        String selectedPath = Global.fileList.get(selectedIndex);
        if (selectedIndex != 0) {
            Global.addFile(selectedPath);
            renderFileList();
        }

        if (executeWindow == null) {
            executeWindow = new ExecuteWindow();
        }
        executeWindow.exec(selectedPath);
        executeWindow.focus();
    }

    void renderFileList() {
        Font font = Font.decode(null);

        for (Component comp : fileListComponents) {
            frame.getContentPane().remove(comp);
        }

        // 文件列表
        int y = 50;
        int startIndex = selectedIndex - 7;
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int indexOffset = 0; indexOffset < 9; ++indexOffset) {
            int index = startIndex + indexOffset;
            if (index >= Global.fileList.size()) {
                break;
            }

            String filePath = Global.fileList.get(index);
            Color background = null;
            if (index == selectedIndex) {
                background = new Color(51, 153, 234);
            }

            int index2 = index;
            MouseListener mouseListener = new MouseListener() {
                int rowIndex = index2;
                @Override
                public void mouseClicked(MouseEvent e) {}
                @Override
                public void mousePressed(MouseEvent e) {
                    if (selectedIndex != rowIndex) {
                        selectedIndex = rowIndex;
                        renderFileList();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
            };

            String shortName = filePath.substring(filePath.lastIndexOf("/") + 1);
            Label label = new Label(shortName);
            label.setFont(font.deriveFont(Font.PLAIN, 20));
            label.setSize(600, 30);
            label.setBackground(background);
            label.setLocation(20, y + 10);
            frame.getContentPane().add(label);
            fileListComponents.add(label);
            label.addMouseListener(mouseListener);

            label = new Label(filePath.replace(shortName, ""));
            label.setFont(font.deriveFont(Font.PLAIN, 12));
            label.setSize(600, 20);
            label.setLocation(20, y + 40);
            label.setBackground(background);
            frame.getContentPane().add(label);
            fileListComponents.add(label);
            label.addMouseListener(mouseListener);

            // 选中背景
            if (background != null) {

                Button execBtn = new Button("执行");
                execBtn.setSize(80, 40);
                execBtn.setLocation(frame.getWidth() - 90, y + 18);
                execBtn.setBackground(background);
                frame.getContentPane().add(execBtn);
                fileListComponents.add(execBtn);
                execBtn.addActionListener(e -> {
                    startExecute();
                });

                label = new Label("");
                label.setSize(800, 70);
                label.setLocation(0, y + 2);
                label.setBackground(background);
                frame.getContentPane().add(label);
                fileListComponents.add(label);
            }

            label = new Label("");
            label.setSize(800, 60);
            label.setLocation(0, y);
            label.setBackground(background);
            frame.getContentPane().add(label);
            fileListComponents.add(label);
            label.addMouseListener(mouseListener);

            y += 60;
        }
    }
}
