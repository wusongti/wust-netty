package com.wust.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * 基于JDK 1.4后的非阻塞IO通信服务
 * Created by WST on 2019/6/27.
 */
public class NIOServer {
    //通道管理器
    private Selector selector;

    /**
     *
     * initServer:初始化服务，准备一个Socket通道，并注册感兴趣的connect事件. <br/>
     *
     * @author wust
     * @param port 需要监听的端口
     * @throws IOException
     * @since
     */
    public void initServer(int port) throws IOException {
        // 获得一个ServerSocket通道
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        // 设置通道为非阻塞
        serverChannel.configureBlocking(false);

        // 将通道与端口绑定关联起来
        serverChannel.socket().bind(new InetSocketAddress(port));

        // 获得一个通道管理器
        this.selector = Selector.open();

        // 注册感兴趣的事件，即accept事件，当该事件到达时，selector.select()会返回，如果该事件没到达selector.select()会一直阻塞。
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }


    /**
     *
     * listen:监听并处理需要的事件. <br/>
     *
     * @author wust
     * @throws IOException
     * @since
     */
    public void listen() throws IOException {
        System.out.println("服务端启动成功.............");
        while (true) {
            //当注册的事件到达时，方法返回；否则,该方法会一直阻塞
            selector.select();

            // 获得selector中选中的项的迭代器，选中的项为注册的事件
            Iterator<?> ite = this.selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = (SelectionKey) ite.next();

                // 删除已选的key,以防重复处理
                ite.remove();

                if (key.isAcceptable()) {// 连接事件发生
                    connect(key);
                } else if (key.isReadable()) {// 读取事件发生
                    read(key);
                }
            }
        }
    }

    /**
     *
     * connect:处理连接. <br/>
     *
     * @author wust
     * @param key
     * @throws IOException
     * @since
     */
    private void connect(SelectionKey key) throws IOException{
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        // 获得和客户端连接的通道
        SocketChannel channel = server.accept();
        // 设置成非阻塞
        channel.configureBlocking(false);

        //在这里可以给客户端发送信息哦
        channel.write(ByteBuffer.wrap(new String("向客户端发送了一条信息").getBytes()));
        //在和客户端连接成功之后，为了可以接收到客户端的信息，需要给通道设置读的权限。
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

        System.out.println("服务端收到信息：" + msg);

        ByteBuffer outBuffer = ByteBuffer.wrap(msg.getBytes());

        // 将消息回送给客户端
        channel.write(outBuffer);
    }

    /**
     * 启动服务端测试
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        NIOServer server = new NIOServer();
        server.initServer(8000);
        server.listen();
    }
}
