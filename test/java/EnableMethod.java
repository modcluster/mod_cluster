import org.apache.commons.httpclient.methods.PostMethod;
public class EnableMethod extends PostMethod {
    public String getName() {
        return "ENABLE-APP";
    }
    public EnableMethod(String uri) {
        super(uri);
    }
} 
