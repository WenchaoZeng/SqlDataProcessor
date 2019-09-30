package com.zwc.sqldataprocessor;

public class MacApp {
    public static void main(String[] args) throws Exception {
        try {
            String cmd = "java -cp ./lib/* com.zwc.sqldataprocessor.MainWindow";
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ex) {
            Global.alert(ex.getMessage());
        }
    }
}
