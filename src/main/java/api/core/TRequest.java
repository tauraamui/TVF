package api.core;

import spark.Request;
import spark.Response;
import spark.Session;

/**
 * Created by alewis on 23/05/2017.
 */
public class TRequest extends TAPIClass {

    public TRequest(Request request, Response response) {
        super(request, response);
    }

    public String getRequestURI() {
        return request.uri();
    }

    public String getRequestURL() { return request.url(); }

    public String getRequestURIParamValue(String paramName) { return request.params(paramName); }

    public Session getSession() { return request.session(); }

    public Object getSessionAttribute(String attributeString) { return request.session().attribute(attributeString); }

    public Request getRequest() { return request; }
}
