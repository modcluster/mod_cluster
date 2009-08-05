import org.apache.commons.httpclient.methods.PostMethod;
public class RemoveMethod extends PostMethod {
    public String getName() {
        return "REMOVE-APP";
    }
    public RemoveMethod(String uri) {
        super(uri);
    }
} 
