package proxy.chunked.iis;

import org.bbottema.javasocksproxyserver.ProxyHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

public class ChunkProxyHandler extends ProxyHandler {
    public static final String connect = "0";
    public static final String write = "1";
    public static final String close = "2";
    public static final byte connectFail= 0x02;

    private String socketId;
    private HttpURLConnection client;
    private URL url = null;
    public ChunkProxyHandler(Socket clientSocket) {
        super(clientSocket);
    }

    @Override
    public void connectToServer(String server, int port) throws IOException {
        if (!handshake(server, port)){
           throw new IOException("connect fail!");
        }

    }

    public boolean handshake(String host, int port)throws IOException{
        try {
            url=new URL(super.getEnvironment().get("proxyUrl").toString());
            socketId=UUID.randomUUID().toString();
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoInput(true);
            client.setDoOutput(true);
            client.setFixedLengthStreamingMode(0);
            client.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
            // id
            // type
            // host
            // port
            client.setRequestProperty("target", String.format("%s,%s,%s,%s",socketId,connect,host,port));
            //client.setChunkedStreamingMode(1024);
            client.connect();
            m_ServerOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    this.write(new byte[]{(byte) b},0,1);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/octet-stream");
                    connection.setRequestProperty("target", String.format("%s,%s",socketId,write));
                    connection.setFixedLengthStreamingMode(len-off);
                    connection.connect();
                    connection.getOutputStream().write(b,off,len);
                    connection.getInputStream();
                    if (!"succesfully".equals(connection.getHeaderField("status"))){
                        throw new IOException("connection broken!");
                    }
                }
            };
            m_ServerInput = client.getInputStream();
            if ("succesfully".equals(client.getHeaderField("status"))){
                if("chunked".equals(client.getHeaderField("Transfer-Encoding"))){
                    if (socketId.equals(readCString(m_ServerInput))){
                        return true;
                    }else {
                        throw new IOException("The first handshake failed");
                    }

                }else {
                    throw new IOException("Transfer-Encoding not is chunked");
                }
            }else {
                throw new IOException("connect fail!");
            }

        }catch(Exception e){
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
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("target", String.format("%s,%s",socketId,close));
            connection.setFixedLengthStreamingMode(0);
            connection.setDoInput(true);
            connection.connect();
            connection.getInputStream();
            connection.disconnect();
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
