package proxy.chunked.iis;

import org.bbottema.javasocksproxyserver.SocksServer;

import java.io.InputStream;

public class Main {

    static InputStream inputStream;
    public static void main(int listenPort,String proxyUrl) throws Throwable {
        SocksServer socksServer =new SocksServer();

        socksServer.getEnv().put("proxyUrl",proxyUrl);
//        socksServer.getEnv().put("proxyUrl","http://localhost:42969/easy.aspx");

        socksServer.start(listenPort,ChunkProxyHandler.class);




    }


}
