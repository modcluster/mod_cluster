import org.apache.commons.httpclient.methods.PostMethod;
public class DumpMethod extends PostMethod {
    public String getName() {
        return "DUMP";
    }
    public DumpMethod(String uri) {
        super(uri);
    }
} 
