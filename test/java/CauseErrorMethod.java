import org.apache.commons.httpclient.methods.PostMethod;
public class CauseErrorMethod extends PostMethod {
    public String getName() {
        return "ERROR";
    }
    public CauseErrorMethod(String uri) {
        super(uri);
    }
} 
