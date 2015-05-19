package com.martiansoftware.boom;


import static com.martiansoftware.boom.Boom.*;
import com.martiansoftware.dumbtemplates.DumbTemplate;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletResponse;
import spark.HaltException;

/**
 *
 * @author mlamb
 */
public class StatusPage {
    
    public static BoomResponse of(HaltException he) {
        return of(he.getStatusCode(), he.getBody());
    }
    
    public static BoomResponse of(Exception e) {
        return of(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    
    public static BoomResponse of(int status, String body) {
        ResourceBundle rb = Boom.r("httpstatus");
        String stext = rb.getString(String.format("SC_%d", status));
        context("status", status);
        context("title", String.format("%d %s", status, stext == null ? "" : stext));
        context("body", body == null ? stext : body);
        // TODO: add stack trace if debug and status==500
        DumbTemplate t = template(String.format("/boom/status/%d.html", status));
        if (t == null) t = template("/boom/status/default.html");
        return new BoomResponse(t.render(context()))
                                        .status(status)
                                        .as(MimeType.HTML);
    }
}
