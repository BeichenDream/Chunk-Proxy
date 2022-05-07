import proxy.chunked.tomcat.ChunkProxyHandler;
import utils.MiTM;

public class Main {
    public static void main(String[] args) throws Throwable {
        MiTM.trustAllHttpsCertificates();
        if (args.length == 3 && (args[2].startsWith("https://")||args[2].startsWith("http://"))){

            String type = args[0];
            int port = Integer.parseInt(args[1]);
            String targetUrl = args[2];

            switch (type.toLowerCase()){
                case "java" :
                    proxy.chunked.tomcat.Main.main(port,targetUrl);
                    break;
                case ".net" :
                    proxy.chunked.iis.Main.main(port,targetUrl);
                    break;
                default:
                    System.out.println("type does not exist");
                    break;
            }


        }else {
            System.out.println("usage: java -jar chunk-Proxy.jar type listenPort targetUrl");
            System.out.println("\ttype");
            System.out.println("\t\t.net");
            System.out.println("\t\tjava");
            System.out.println("learn");
            System.out.println("\tjava -jar chunk-Proxy.jar java 1088 http://10.10.10.1:8080/proxy.jsp");
        }
    }
}
