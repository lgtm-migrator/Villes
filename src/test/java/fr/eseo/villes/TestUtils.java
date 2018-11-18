package fr.eseo.villes;

import fr.klemek.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ReadListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {

    public static final Level LOG_LEVEL = Level.INFO;
    public static final String ADMIN_EMAIL = "adminemail";
    public static final Map.Entry<String, String> langEntry = new AbstractMap.SimpleEntry<>("testKey", "testValue");
    public static boolean loggerInitialized;
    public static boolean initContextInitialized;

    private TestUtils() {

    }

    public static HttpServletRequest createMockRequest(String method, String URI, Map<String, String> parameters,
                                                       Map<String, String> headers, Map<String, Object> session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(URI);
        StringBuilder data = new StringBuilder();
        if (parameters != null) {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                when(request.getParameter(param.getKey())).thenReturn(param.getValue());
                data.append(param.getKey()).append("=").append(param.getValue()).append("&");
            }
        }

        ServletContext contextMock = mock(ServletContext.class);
        when(request.getServletContext()).thenReturn(contextMock);
        when(contextMock.getInitParameter(Mockito.eq("app.path"))).thenReturn("/");

        HttpSession sessionMock = mock(HttpSession.class);
        when(request.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute(Mockito.anyString())).thenReturn(null);
        if (session != null) {
            for (Map.Entry<String, Object> param : session.entrySet()) {
                when(sessionMock.getAttribute(param.getKey())).thenReturn(param.getValue());
            }
        }
        try {
            doReturn(new ServletInputStream() {
                private final ByteArrayInputStream stream = new ByteArrayInputStream(data.toString().getBytes());

                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return stream.available() > 0;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return stream.read();
                }
            }).when(request).getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(request.getParameterMap()).thenReturn(new HashMap<>());
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                when(request.getHeader(header.getKey())).thenReturn(header.getValue());
            }
        }
        return request;
    }

    public static HttpServletResponse createMockResponse(StringWriter stringWriter) {
        HttpServletResponse response = mock(HttpServletResponse.class);
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
        } catch (IOException ignored) {
        }
        return response;
    }

    public static JSONObject getResponseAsJson(StringWriter stringWriter) {
        return new JSONObject(stringWriter.toString());
    }

    public static boolean prepareTestClass() {
        if (!TestUtils.loggerInitialized) {
            Logger.init("logging.properties", TestUtils.LOG_LEVEL);
            TestUtils.loggerInitialized = true;
        }

        if (!TestUtils.initContextInitialized) {
            try {
                Class<?> initContextListener = Class.forName("fr.eseo.villes.ContextListener");
                Field appPath = initContextListener.getDeclaredField("appPath");
                appPath.setAccessible(true);
                appPath.set(initContextListener, "/");
            } catch (Exception e) {
                Logger.log(e);
                return false;
            }
            Logger.log(Level.INFO, "App path initialized");
            TestUtils.initContextInitialized = true;
        }
        return true;
    }
}