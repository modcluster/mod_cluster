import org.apache.commons.httpclient.methods.PostMethod;
public class StopMethod extends PostMethod {
    public String getName() {
        return "STOP-APP";
    }
    public StopMethod(String uri) {
        super(uri);
    }
} 
