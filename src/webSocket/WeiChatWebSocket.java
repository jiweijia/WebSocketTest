package webSocket;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/WeiChat")
public class WeiChatWebSocket {

    //静态变量，用来记录当前在线连接数，线程安全
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端和单一客户端通信的话，可以用map来存放，其中key可以为用户标志。
    private static CopyOnWriteArraySet<WeiChatWebSocket> webSocketSet = new CopyOnWriteArraySet<WeiChatWebSocket>();
    //定义一个记录客户端的聊天昵称
    private final String nickname;
    //与某个客户端的连接会话，需要通过他来给客户端发送数据
    private Session session;
    
    public WeiChatWebSocket(){
	nickname="访客"+onlineCount.getAndIncrement();
    }
    /*
     * 使用@Onopen注解表示当客户端链接成功后的回调。参数Session是可选参数；
     * 这个session是websocket的规范的会话，表示一次会话。并非httpsession。
     */
    @OnOpen
    public void onOpen(Session session){
	this.session=session;
	webSocketSet.add(this);
	String message= String.format("[%s,%s]", nickname,"加入聊天室");
	broadcast(message);
	System.out.println("onOpen");
    }
    /*
     * 使用@OnMessage注解表示客户端发送信息后的回调，第一个参数表示用户发送的数据。
     */
    @OnMessage
    public void onMessage(String message,Session session){
	System.out.println(this.session==session);
	broadcast(String.format("%s:%s", nickname,filter(message)));
    }
    /*
     * 用户断开链接后的回调函数，注意这个方法必须是客户端断开了链接方法后才会回调
     */
    @OnClose
    public void onClose(){
	webSocketSet.remove(this);
	String message=String.format("[%s,%s]", nickname,"离开了聊天室");
	broadcast(message);
    }
    //对用户的消息可以做一些过滤请求，如屏蔽关键字等等
    private String filter(String message) {
	if(message==null){
	    return null;
	}
	return message;
    }
    //完成群发
    private void broadcast(String info) {
	for(WeiChatWebSocket w:webSocketSet){
	    try{
		synchronized(WebSocketDemo.class){
		    w.session.getBasicRemote().sendText(info);
		}
	    }catch(IOException e){
		System.out.println("向客户端"+w.nickname+"发送信息失败");
		webSocketSet.remove(w);
		try {
		    w.session.close();
		} catch (Exception e2) {
		    e2.printStackTrace();
		}
		String message = String.format("[%s,%s]", w.nickname,"已经断开链接");
		broadcast(message);
	    }
	}
    }
}
