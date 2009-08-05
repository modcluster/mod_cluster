import org.apache.commons.httpclient.methods.PostMethod;
public class StatusMethod extends PostMethod {
    public String getName() {
        return "STATUS";
    }
    public StatusMethod(String uri) {
        super(uri);
    }
} 
