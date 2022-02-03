import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ServerSocketStream {
    public Socket socketp;
    public Socket socketc;
    public OutputStream outputStreamp;
    public OutputStream outputStreamc;
    public BufferedReader inputStreamp;
    public BufferedReader inputStreamc;
    public String responsep;
    public String responsec;


    public ServerSocketStream(Socket socketp,Socket socketc) {
        this.socketp = socketp;
        this.socketc = socketc;
        try {
            socketp.setSoTimeout(Integer.MAX_VALUE);
            socketc.setSoTimeout(Integer.MAX_VALUE);
            this.inputStreamp = new BufferedReader(new InputStreamReader(socketp.getInputStream(),
                    StandardCharsets.UTF_8));
            this.inputStreamc = new BufferedReader(new InputStreamReader(socketc.getInputStream(),
                    StandardCharsets.UTF_8));
            this.outputStreamp = socketp.getOutputStream();
            this.outputStreamc = socketc.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ServerSocketStream: 建立SocketStream失败！");
        }

    }

    public void onCreate() {

        //Parent->Child
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    //连接是否正常
                    if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected()) {
                        try {
                            System.out.println("ServerSocketStream: Parent->Child：开始监听Parent客户端的请求...");
                            //检查是否读取到来源于Parent的信息
                            if ((responsep = inputStreamp.readLine()) != null) {
                                System.out.println("ServerSocketStream: Parent->Child：读取到Parent消息：" + responsep);
                                if(Objects.equals(responsep, "BeatData")){
                                    System.out.println("ServerSocketStream: Parent心跳数据");
                                    continue;
                                }

                                //向Child发送信息
                                if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected() && outputStreamc != null) {
                                    outputStreamc.write((responsep+"\n").getBytes(StandardCharsets.UTF_8));
                                    outputStreamc.flush();
                                    System.out.println("ServerSocketStream: Parent->Child：发送成功！");
                                }else{
                                    System.out.println("ServerSocketStream: Parent->Child：Child连接错误！");
                                    onFailedConnection();
                                    return;
                                }

                            }else{
                                System.out.println("ServerSocketStream: 客户端连接断开");
                                onFailedConnection();
                                return;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("ServerSocketStream: 客户端连接断开");
                            onFailedConnection();
                            return;
                        } catch (Exception e) {
                            //字符串切割异常
                            e.printStackTrace();
                            return;
                        }

                    } else {
                        System.out.println("ServerSocketStream: Socket连接断开！");
                        onFailedConnection();
                        return;
                    }
                }

            }
        }).start();

        //Child->Parent
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    //连接是否正常
                    if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected()) {
                        try {
                            System.out.println("ServerSocketStream: Child->Parent：开始监听Child客户端的请求...");
                            //检查是否读取到来源于Child的信息
                            if ((responsec = inputStreamc.readLine()) != null) {
                                System.out.println("ServerSocketStream: Child->Parent：读取到Child消息：" + responsec);
                                if(Objects.equals(responsec, "BeatData")){
                                    System.out.println("ServerSocketStream: Child心跳数据");
                                    continue;
                                }
                                //向Parent发送信息
                                if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected() && outputStreamp != null) {
                                    outputStreamp.write((responsec+"\n").getBytes(StandardCharsets.UTF_8));
                                    outputStreamp.flush();
                                    System.out.println("ServerSocketStream: Child->Parent：发送成功！");
                                }else{
                                    System.out.println("ServerSocketStream: Child->Parent：Parent连接错误！");
                                    onFailedConnection();
                                    return;
                                }

                            }else{
                                System.out.println("ServerSocketStream: 监听连接流失败");
                                onFailedConnection();
                                return;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("ServerSocketStream: 监听连接流失败");
                            onFailedConnection();
                            return;
                        } catch (Exception e) {
                            //字符串切割异常
                            System.out.println("ServerSocketStream: 字符串切割异常");
                            e.printStackTrace();
                            return;
                        }

                    } else {
                        System.out.println("ServerSocketStream: Socket连接断开！");
                        onFailedConnection();
                        return;
                    }
                }

            }
        }).start();

    }

    public void onFailedConnection() {
        //当有一方连接失败时，关闭连接
        try {
            //向Child发送错误信息
            if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected() && outputStreamc != null) {
                outputStreamc.write(("ConnectionisClosed\n").getBytes(StandardCharsets.UTF_8));
                outputStreamc.flush();
            }
            //向Parent发送信息aa
            if (socketp != null && socketc != null && socketp.isConnected() && socketc.isConnected() && outputStreamp != null) {
                outputStreamp.write(("ConnectionisClosed\n").getBytes(StandardCharsets.UTF_8));
                outputStreamp.flush();
            }
            outputStreamc.close();
            outputStreamp.close();
            inputStreamc.close();
            inputStreamp.close();
            socketp.close();
            socketc.close();
            System.out.println("ServerSocketStream: 连接已经全部断开！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
