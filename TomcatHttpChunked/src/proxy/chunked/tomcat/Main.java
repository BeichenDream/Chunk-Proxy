package proxy.chunked.tomcat;

import org.bbottema.javasocksproxyserver.SocksServer;

import java.io.InputStream;

public class Main {


    public static void main(int listenPort,String proxyUrl) throws Throwable {
        SocksServer socksServer =new SocksServer();

        socksServer.getEnv().put("proxyUrl",proxyUrl);

        socksServer.start(listenPort,ChunkProxyHandler.class);




    }


}
