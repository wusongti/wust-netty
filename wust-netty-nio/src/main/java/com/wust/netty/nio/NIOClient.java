package com.wust.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 基于JDK 1.4后的非阻塞IO通信客户端
 * Created by WST on 2019/6/27.
 */
public class NIOClient {
    //通道管理器
    private Selector selector;

    /**
     *
     * initClient:初始化客户端服务，准备一个Socket通道，并注册感兴趣的connect事件. <br/>
     *
     * @author wust
     * @param ip
     * @param port
     * @throws IOException
     * @since
     */
    public void initClient(String ip,int port) throws IOException {
        // 获得一个Socket通道
        SocketChannel channel = SocketChannel.open();

        // 设置通道为非阻塞
        channel.configureBlocking(false);

        // 获得一个通道管理器
        this.selector = Selector.open();

        // 客户端连接服务器,其实方法执行并没有实现连接，需要在listen（）方法中调
        // 用channel.finishConnect();才能完成连接
        channel.connect(new InetSocketAddress(ip,port));

        // 注册感兴趣的事件，将通道管理器和该通道绑定
        channel.register(selector, SelectionKey.OP_CONNECT);
    }



    /**
     *
     * listen:监听并处理通道管理器上面的事件. <br/>
     *
     * @author wust
     * @throws IOException
     * @since
     */
    public void listen() throws IOException {
        System.out.println("客户端启动成功...............");
        while (true) {
            // Selects a set of keys whose corresponding channels are ready for I/O operations.
            selector.select();

            // Returns this selector's selected-key set.
            Set<SelectionKey> selectedKeys = this.selector.selectedKeys();

            // 迭代
            Iterator<?> ite = selectedKeys.iterator();
            while (ite.hasNext()) {
                SelectionKey key = (SelectionKey) ite.next();

                // 删除已选的key,以防重复处理
                ite.remove();

                if (key.isConnectable()) {// 连接事件发生
                    connect(key);
                } else if (key.isReadable()) {// 读取事件发生
                    read(key);
                }
            }
        }
    }

    /**
     *
     * connect:与通道建立连接. <br/>
     *
     * @author wust
     * @param key
     * @throws IOException
     * @since
     */
    private void connect(SelectionKey key) throws IOException{
        // 获取通道
        SocketChannel channel = (SocketChannel) key.channel();

        // 如果正在连接，则完成连接
        if(channel.isConnectionPending()){
            // Finishes the process of connecting a socket channel.
            channel.finishConnect();
        }

        // 设置成非阻塞
        channel.configureBlocking(false);

        // 向通道写入消息，即给服务器发送消息
        channel.write(ByteBuffer.wrap(new String("我是客户端").getBytes()));

        // 在和服务端连接成功之后，为了可以接收到服务端的信息，需要给通道设置读的权限。
        channel.register(this.selector, SelectionKey.OP_READ);
    }


    /**
     *
     * read:读取信息. <br/>
     *
     * @author wust
     * @param key
     * @throws IOException
     * @since
     */
    private void read(SelectionKey key) throws IOException{
        // 服务器可读取消息:得到事件发生的Socket通道
        SocketChannel channel = (SocketChannel) key.channel();

        // 创建读取的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(10);

        channel.read(buffer);

        byte[] data = buffer.array();

        String msg = new String(data).trim();

        System.out.println("客户端收到信息：" + msg);

        ByteBuffer outBuffer = ByteBuffer.wrap(msg.getBytes());

        // 将消息回送给客户端
        channel.write(outBuffer);
    }


    /**
     * 启动客户端测试
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        NIOClient client = new NIOClient();
        client.initClient("localhost",8000);
        client.listen();
    }
}
