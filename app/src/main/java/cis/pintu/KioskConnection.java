package cis.pintu;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

interface  MessageCallBackHelper{
    void onMessage(IoSession session, String data);
    void onMessageSent(IoSession session);
    void onError(IoSession ioSession);
    void onConnected(IoSession session);
}

public class KioskConnection extends IoHandlerAdapter {

    /** The connector */
    private IoConnector connector;

    /** The session */
    private static IoSession session;
    private static KioskConnection kioskConnection;
    MessageCallBackHelper messageCallBackHelper;
    private boolean isConnected;

    private KioskConnection (){
            //buildConnection();
    }

    public void disposeConnection(){
        try {
            if (session != null && connector != null) {
                connector.dispose();
                isConnected=false;
            }
        }catch (Exception er){
            isConnected=false;
        }
    }

    public void buildConnection(String ip,int port){
        connector = new NioSocketConnector();
        connector.setHandler(this);
        connector.getSessionConfig().setMaxReadBufferSize(2000);
        connector.getSessionConfig().setMinReadBufferSize(1000);

        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        ConnectFuture connFuture = connector.connect(new InetSocketAddress(ip, port));
        connFuture.awaitUninterruptibly();
        session = connFuture.getSession();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setMessageCallBackHelper(MessageCallBackHelper messageCallBackHelper) {
        this.messageCallBackHelper = messageCallBackHelper;
    }

    public void sendData(String data){
        if (session!=null){
                if (session.isActive() && session.isConnected()) {
                    session.write(data.getBytes());
                    session.write(new StringBuffer(data));
                }else {
                    messageCallBackHelper.onError(session);
                }
        }
    }

    public static synchronized KioskConnection getInstance(){
            if (kioskConnection!=null){
                return kioskConnection;
            }else{
                return kioskConnection=new KioskConnection();
            }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public  void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        //cause.printStackTrace();
            if (messageCallBackHelper!=null)
            messageCallBackHelper.onError(session);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public  void messageReceived(IoSession session, Object message) throws Exception {
        isConnected=true;
        //long diff = System.currentTimeMillis()-Long.valueOf(message.toString());
        //System.out.println(" "+message+" from session "+session.getId());
        if (messageCallBackHelper!=null)
        messageCallBackHelper.onMessage(session,message.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  void messageSent(IoSession session, Object message) throws Exception {
                if (messageCallBackHelper!=null)
                messageCallBackHelper.onMessageSent(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  void sessionClosed(IoSession session) throws Exception {
            if (messageCallBackHelper!=null)
            messageCallBackHelper.onError(session);

        isConnected=false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  void sessionCreated(IoSession session) throws Exception {
            isConnected=true;

          if (messageCallBackHelper!=null)
          messageCallBackHelper.onConnected(session);


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  void sessionIdle(IoSession session, IdleStatus status) throws Exception {
         isConnected=true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  void sessionOpened(IoSession session) throws Exception {
            isConnected=true;

            if (messageCallBackHelper!=null)
            messageCallBackHelper.onConnected(session);
    }

}



