package info.tinyapps.huges.data;

/**
 * AWS response data wrapper class
 */
public class ResponseData {
    String result;

    public ResponseData() {
    }

    public ResponseData(String greetings) {
        this.result = greetings;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String greetings) {
        this.result = greetings;
    }
}
