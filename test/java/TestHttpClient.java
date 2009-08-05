import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

public class  TestHttpClient
{

   public static String JVMRoute = System.getProperty("JVMRoute", "node1");
   public static String Host = System.getProperty("Host", "localhost");
   public static String Factor = "50";
    /**
     *  
     * Usage:
     *          java TestHttpClient http://mywebserver:80/ test
     * 
     *  @param args command line arguments
     *                 Argument 0 is a URL to a web server
     *                 Argument 1 is the command to execute.
     * 
     */
    public static void main(String[] args) throws Exception
        {
                if (args.length == 2)
                    runit(args[0], args[1]);
                else if (args.length == 3) {
                    Factor = args[2];
                    System.out.println("Using factor: " + Factor);
                    runit(args[0], args[1]);
                } else {
                    System.err.println("missing command line arguments");
                    System.exit(1);
                }
        }
        public static int runit(String URL, String command) throws Exception
        {

                HttpClient httpClient = new HttpClient(); 
                PostMethod pm = null;
                if (command.compareToIgnoreCase("ENABLE")==0) {
                    pm = (PostMethod) new EnableMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("context", "/myapp");
                    System.out.println("ENABLE");
                }
                else if (command.compareToIgnoreCase("DISABLE")==0) {
                    pm = (PostMethod) new DisableMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("context", "/myapp");
                }
                else if (command.compareToIgnoreCase("STOP")==0) {
                    pm = (PostMethod) new StopMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("context", "/myapp");
                }
                else if (command.compareToIgnoreCase("REMOVE")==0) {
                    pm = (PostMethod) new RemoveMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("context", "/hisapp");
                }
                else if (command.compareToIgnoreCase("CONFIG")==0) {
                    pm = (PostMethod) new ConfigMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("Domain", "domain1");
                    pm.addParameter("Host", Host);
                    pm.addParameter("Port", "8009");
                    pm.addParameter("Type", "ajp");
                    // pm.addParameter("Reversed", "yes");
                    pm.addParameter("Context", "/hisapp,/ourapp");
                }
                else if (command.compareToIgnoreCase("DUMP")==0) {
                    pm = (PostMethod) new DumpMethod(URL);
                }
                else if (command.compareToIgnoreCase("STATUS")==0) {
                    System.out.println("STATUS factor: " + Factor);
                    pm = (PostMethod) new StatusMethod(URL);
                    pm.addParameter("JVMRoute", JVMRoute);
                    pm.addParameter("Load", Factor);
                }
                else if (command.compareToIgnoreCase("ERROR")==0) {
                    pm = (PostMethod) new CauseErrorMethod(URL);
                }
                else
                    pm = (PostMethod) new InfoMethod(URL);

                System.out.println("Connecting to " + URL);

                Integer connectionTimeout = 40000;
                pm.getParams().setParameter("http.socket.timeout", connectionTimeout);
                pm.getParams().setParameter("http.connection.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.socket.timeout", connectionTimeout);
                httpClient.getParams().setParameter("http.connection.timeout", connectionTimeout);

                int httpResponseCode = 0;
                try {
                    httpResponseCode = httpClient.executeMethod(pm);
                    System.out.println("response: " + httpResponseCode);
                    System.out.println("response: " + pm.getStatusLine());
                    if (httpResponseCode == 500) {
                        System.out.println(pm.getResponseHeader("Version"));
                        System.out.println(pm.getResponseHeader("Type"));
                        System.out.println(pm.getResponseHeader("Mess"));
                    }
                    int len = (int) pm.getResponseContentLength();
                    System.out.println("response:\n" + pm.getResponseBodyAsString(len)); 
                } catch(HttpException e) { 
                    e.printStackTrace();
                }
                return httpResponseCode;
        }
}
