package ca.nrc.cadc.net;

/**
 * Created by jeevesh on 2018-10-17.
 */
public class FileContent {

    String content;
    String contentType;

    public FileContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
