import org.apache.commons.httpclient.methods.PostMethod;
public class ConfigMethod extends PostMethod {
    public String getName() {
        return "CONFIG";
    }
    public ConfigMethod(String uri) {
        super(uri);
    }
} 
