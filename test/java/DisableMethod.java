import org.apache.commons.httpclient.methods.PostMethod;
public class DisableMethod extends PostMethod {
    public String getName() {
        return "DISABLE-APP";
    }
    public DisableMethod(String uri) {
        super(uri);
    }
} 
