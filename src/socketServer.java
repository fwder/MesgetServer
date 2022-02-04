import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class socketServer {

    public static Map<String, Socket> map;
    public static ServerSocket ss = null;
    public static Socket sk = null;

    public static void main(String[] args) {
        while (true) {
            map = new HashMap<String, Socket>();
            ss = null;
            sk = null;
            try {
                ss = new ServerSocket(9011);
                System.out.println("创建Socket服务器中...完毕。");
                //服务器端一直监听这个端口，等待客户端的连接
                while (true) {
                    System.out.println("等待客户端连接....");
                    sk = ss.accept();  //当有客户端连接时，产生阻塞
                    System.out.println("有客户端连接... 来源：" + sk.getInetAddress() + ":" + sk.getLocalPort() + " 客户端连接成功。");


                    //设置监听为INT_MAX
                    sk.setSoTimeout(Integer.MAX_VALUE);
                    //读取请求信息
                    BufferedReader inputStream = new BufferedReader(new InputStreamReader(sk.getInputStream(),
                            StandardCharsets.UTF_8));
                    //检查是否读取到请求连接信息
                    System.out.println("读取请求信息...");
                    String response;
                    if ((response = inputStream.readLine()) != null) {
                        System.out.println("onListenServer: 读取到请求信息：" + response);
                    }else{
                        System.out.println("onListenServer: 读取到请求信息为null，跳过本次请求");
                        continue;
                    }
                    //判断是Child还是Parent
                    String func = stringMatch("func", response);
                    String type = stringMatch("type", response);

                    //Child就连接保活就行，丢到map里面去
                    if (Objects.equals(func, "onCreateConnection") && (Objects.equals(type, "Child"))) {
                        //这里需要先验证map里对应的Child的Socket连接是否还存在
                        String mcname = stringMatch("MCName", response);
                        Socket spp = map.get(mcname);
                        //检查map里是否有该Child的Socket连接
                        if (spp == null) {
                            map.put(mcname, sk);
                            System.out.println("Child设备：" + mcname + " 连接成功");
                        } else {
                            System.out.println("Child设备：" + mcname + " 更换连接中。。。");
                            spp.close();
                            map.remove(mcname);
                            map.put(mcname, sk);
                            System.out.println("Child设备：" + mcname + " 连接成功");
                        }
                        continue;
                    }

                    //如果是Parent连接，把在线的Child的Socket连接丢过来
                    if (Objects.equals(func, "onCreateConnection") && Objects.equals(type, "Parent") && !Objects.equals(stringMatch("ChildName", response), "")) {
                        //检查Child是否在线
                        String ChildName = stringMatch("ChildName", response);
                        Socket sc = null;
                        sc = map.get(ChildName);
                        if (sc != null && sc.isConnected() && isChildConnectionExist(sc)) {
                            //发送返回信息
                            OutputStream outputStream = sk.getOutputStream();
                            if (sk != null && sk.isConnected() && outputStream != null) {
                                outputStream.write(("Success\n").getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            }
                            sc.setSoTimeout(Integer.MAX_VALUE);
                            //创建Child和Parent之间的连接
                            ServerSocketStream sss = new ServerSocketStream(sk, sc); // 创建Parent到Child的Stream
                            sss.onCreate();
                            System.out.println("Parent: " + sk.getInetAddress() + ":" + sk.getLocalPort() + " Child: " + sc.getInetAddress() + ":" + sc.getLocalPort() + " MCName: " + ChildName + " 成功建立连接！");
                        } else {
                            //Child设备不在线，断开连接
                            System.out.println("Child设备不在线！");
                            //删除map中离线的Child的Socket记录
                            map.remove(ChildName);
                            //发送返回信息
                            OutputStream outputStream = sk.getOutputStream();
                            if (sk != null && sk.isConnected() && outputStream != null) {
                                outputStream.write(("ChildConnectionIsFailed\n").getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            }
                            sk.close();
                        }
                        continue;
                    }
                    sk.close();
                    System.out.println("请求信息错误，断开连接...");
                }
            } catch (Exception ex) {
                try {
                    ss.close();
                    sk.close();
                    ss = null;
                    sk = null;
                    map = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("主程序抛出，请检查...");
            }
        }
    }

    //切割字符串
    public static String stringMatch(String type, String s) {
        try {
            List<String> results = new ArrayList<String>();
            Pattern p = Pattern.compile("<" + type + ":(.*?)>");
            Matcher m = p.matcher(s);
            while (!m.hitEnd() && m.find()) {
                results.add(m.group(1));
            }
            System.out.println("stringMatch: 切割结果：" + results.get(0));
            return results.get(0);
        } catch (Exception e) {
            System.out.println("stringMatch: 字符串切割错误！");
            return "";
        }
    }

    public static boolean isChildConnectionExist(Socket spp) {
        //检查Child的Socket连接是否正常
        try {
            spp.sendUrgentData(0xFF);
        } catch (Exception ex) {
            //连接异常
            return false;
        }
        return true;
    }

}
