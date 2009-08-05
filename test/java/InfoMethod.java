import org.apache.commons.httpclient.methods.PostMethod;
public class InfoMethod extends PostMethod {
    public String getName() {
        return "INFO";
    }
    public InfoMethod(String uri) {
        super(uri);
    }
} 
