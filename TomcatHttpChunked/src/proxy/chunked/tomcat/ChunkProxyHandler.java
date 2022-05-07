package proxy.chunked.tomcat;

import org.bbottema.javasocksproxyserver.ProxyHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

import static utils.JavaUnsafe.setFieldValue;

public class ChunkProxyHandler extends ProxyHandler {
    public static final byte connectSucceed = 0x01;
    public static final byte connectFail= 0x02;

    private HttpURLConnection client;
    public ChunkProxyHandler(Socket clientSocket) {
        super(clientSocket);
    }

    @Override
    public void connectToServer(String server, int port) throws IOException {
        if (handshake()){
            m_ServerOutput.write(server.getBytes());
            m_ServerOutput.write(0x00);
            m_ServerOutput.write(String.valueOf(port).getBytes());
            m_ServerOutput.write(0x00);
            m_ServerOutput.flush();
            if (m_ServerInput.read()!=connectSucceed){
                close();
                String errMsg=readCString(m_ServerInput);
                LOGGER.print("connectIOExceptionMsg: "+errMsg);
                throw new IOException(errMsg);
            }
        }

    }

    public boolean handshake()throws IOException{
        try {
            URL url=new URL(super.getEnvironment().get("proxyUrl").toString());
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoOutput(true);
            client.setDoInput(true);
            client.setRequestProperty("Transfer-Encoding","chunked");
            client.setRequestProperty("Content-Type", "application/octet-stream");
            client.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

            client.setChunkedStreamingMode(1024);
            client.connect();

            m_ServerOutput = client.getOutputStream();

            byte[] handshake = makeHandshake();

            m_ServerOutput.write(handshake);
            m_ServerOutput.flush();

            setFieldValue(m_ServerOutput,"closed",true);

            client.setReadTimeout(5000);

            m_ServerInput=client.getInputStream();

            setFieldValue(m_ServerOutput,"closed",false);

            client.setReadTimeout(0);

            byte[] serverHandshake = readInputStream(m_ServerInput,16);

            if (serverHandshake!=null && Arrays.equals(handshake,serverHandshake)){
                m_ServerOutput.write(handshake,0,8);
                m_ServerOutput.flush();
                byte[] serverHandshake2 = readInputStream(m_ServerInput,8);
                if (serverHandshake2!=null && Arrays.equals(Arrays.copyOf(handshake,8),serverHandshake2)){
                    return true;
                }else {
                    throw new IOException("第二次握手协商失败");
                }
            }else {
                throw new IOException("第一次握手协商失败");
            }
        }catch(Exception e){
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }



    public String readCString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte c;
        while ((c= (byte) inputStream.read())!=0){
            buffer.write(c);
        }
        return new String(buffer.toByteArray());
    }

    @Override
    public void closeOther() {
        try{
            if (client!=null){
                client.disconnect();
            }
        }catch (Exception e){

        }
    }

    private static byte[] readInputStream(InputStream inputStream, int len)throws IOException{
        int readNumber = 0;
        int offset = 0;
        byte[] buffer = new byte[len];
        while(offset<len&&(readNumber=(inputStream.read(buffer,offset,len)))>0){
            offset+=readNumber;
        }
        if(offset==len){
            return buffer;
        }
        return null;
    }

    public static byte[] makeHandshake(){
        try {
            byte[] bytes = UUID.randomUUID().toString().getBytes();
            MessageDigest messageDigest=MessageDigest.getInstance("MD5");
            messageDigest.update(bytes, 0, bytes.length);
            bytes =  messageDigest.digest();
            if (bytes.length>16){
                bytes = Arrays.copyOf(bytes, 16);
            }
            return bytes;
        }catch (Exception e){
            return null;
        }
    }


}
