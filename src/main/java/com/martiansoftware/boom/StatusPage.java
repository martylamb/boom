package com.martiansoftware.boom;


import static com.martiansoftware.boom.Boom.*;
import com.martiansoftware.dumbtemplates.DumbTemplate;
import java.util.Map;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;

/**
 *
 * @author mlamb
 */
public class StatusPage {
    private static final Logger log = LoggerFactory.getLogger(StatusPage.class);
    
    public static BoomResponse of(HaltException he) {
        log.warn("Halt({}) : {} requested {}", he.getStatusCode(), request().ip(), request().pathInfo());
        return of(he.getStatusCode(), he.getBody(), false);
    }
    
    public static BoomResponse of(Exception e) {
        log(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        return of(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), false);
    }
    
    public static BoomResponse of(int status, String body) {
        return of(status, body, true);
    }
    
    private static BoomResponse of(int status, String body, boolean logErr) {
        if (logErr && isServerError(status)) {
            log(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, new Exception());
        }
        ResourceBundle rb = Boom.r("httpstatus");
        String stext = rb.getString(String.format("SC_%d", status));
        
        if ("application/json".equals(request().headers("Accept"))) {
            Map<String, Object> result = new java.util.TreeMap<>();
            result.put("status", status);
            result.put("status_desc", stext);
            result.put("message", body);
            return json(result).status(status);
        } else if ("text/plain".equals(request().headers("Accept"))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ");
            sb.append(status);
            sb.append("\nStatus Description: ");
            sb.append(stext == null ? "" : stext);
            sb.append("\nMessage: ");
            sb.append(body);
            sb.append("\n");
            return new BoomResponse(sb.toString()).status(status).as(MimeType.TEXT);
        } else {
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
    
    private static boolean isServerError(int status) {
        return status >= 500 && status <= 599;
    }
    
    private static void log(int status, Exception e) {
        log.error(status + ": " + e.getMessage(), e);
    }
}
