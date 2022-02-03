import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class socketChild extends Thread {
    public Socket socket;
    public String response;
    public OutputStream outputStream;

    public socketChild(Socket socket) {
        this.socket = socket;
    }

    public void run() {

        if (socket != null && socket.isConnected()) {
            try {
                socket.setSoTimeout(Integer.MAX_VALUE);
                System.out.println("socketChild: 来源：" + socket.getInetAddress() + ":" + socket.getLocalPort() + " - 开始监听Child客户端的Beta心跳请求...");

                BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                        StandardCharsets.UTF_8));
                outputStream = socket.getOutputStream();
                while (true) {
                    //检查是否读取到信息
                    if ((response = inputStream.readLine()) != null) {
                        System.out.println("socketChild: 来源：" + socket.getInetAddress() + ":" + socket.getLocalPort() + " - 读取到消息：" + response);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("socketChild: 客户端断开连接，退出线程...");
            }

        } else {
            System.out.println("socketChild: 客户端断开连接，退出线程...");
        }

    }

    public static String stringMatch(String type, String s) throws Exception {
        List<String> results = new ArrayList<String>();
        Pattern p = Pattern.compile("<" + type + ":([\\w/\\.]*)>");
        Matcher m = p.matcher(s);
        while (!m.hitEnd() && m.find()) {
            results.add(m.group(1));
        }
        return results.get(0);
    }

}