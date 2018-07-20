package io.github.ydq.jquery;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import sun.net.www.protocol.https.Handler;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class JQuery {

    private static final String EMPTY = "";

    public enum Method {
        GET, POST, PUT, DELETE, HEAD
    }

    @AllArgsConstructor
    public enum ContentType {
        FORMDATA("application/x-www-form-urlencoded"), JSON("application/json"), XML("application/xml");
        @Getter
        String meta;
    }

    @Data
    @NoArgsConstructor
    public static class JQueryResponse {
        Boolean                   status;
        String                    content;
        Map<String, List<String>> headers;

        public JQueryResponse(Boolean status, String content) {
            this.status = status;
            this.content = content;
        }

        public Map<String, String> cookie() {
            if (this.headers != null) {
                List<String> cookieStrs = headers.get("Set-Cookie");
                if (cookieStrs != null && cookieStrs.size() > 0) {
                    var cookie = new HashMap<String, String>();
                    cookieStrs.forEach(str -> {
                        String[] cookieKV = str.split(";")[0].trim().split("=");
                        cookie.put(cookieKV[0], cookieKV[1]);
                    });
                    return cookie;
                }
            }
            return Collections.emptyMap();
        }
    }

    private static ThreadLocal<String> CHARSET = ThreadLocal.withInitial(() -> "UTF-8");

    private static ThreadLocal<String> USER_AGENT = ThreadLocal.withInitial(() -> "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");

    private static ThreadLocal<String> REFERER = ThreadLocal.withInitial(() -> "");

    private static ThreadLocal<Integer> TIMEOUT = ThreadLocal.withInitial(() -> 5000);

    private static ThreadLocal<Method> METHOD = ThreadLocal.withInitial(() -> Method.GET);

    private static ThreadLocal<ContentType> CONTENT_TYPE = ThreadLocal.withInitial(() -> ContentType.FORMDATA);

    private static ThreadLocal<Map<String, String>> HEADER = ThreadLocal.withInitial(HashMap::new);

    private static ThreadLocal<Map<String, String>> COOKIE = ThreadLocal.withInitial(HashMap::new);

    private static Boolean isBlank(CharSequence cs) {
        int csLen;
        if (cs == null || (csLen = cs.length()) == 0) {
            return Boolean.TRUE;
        }
        for (int i = 0; i < csLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public static class $ {

        public static JQueryResponse get(@NonNull String url) {
            return get(url, EMPTY, null);
        }

        public static JQueryResponse get(@NonNull String url, @NonNull FormData formData) {
            return get(url, formData.toString(), null);
        }

        public static JQueryResponse get(@NonNull String url, @NonNull FormData formData, Consumer<JQueryResponse> consumer) {
            return get(url, formData.toString(), consumer);
        }

        public static JQueryResponse get(@NonNull String url, String data) {
            return get(url, data, null);
        }

        public static JQueryResponse get(@NonNull String url, Consumer<JQueryResponse> consumer) {
            return get(url, EMPTY, consumer);
        }

        public static JQueryResponse get(@NonNull String url, String data, Consumer<JQueryResponse> consumer) {
            return ajax(url, data, consumer);
        }

        public static JQueryResponse post(@NonNull String url) {
            return post(url, EMPTY, null);
        }

        public static JQueryResponse post(@NonNull String url, @NonNull FormData formData) {
            return post(url, formData.toString(), null);
        }

        public static JQueryResponse post(@NonNull String url, @NonNull FormData formData, Consumer<JQueryResponse> consumer) {
            return post(url, formData.toString(), consumer);
        }

        public static JQueryResponse post(@NonNull String url, String data) {
            return post(url, data, null);
        }

        public static JQueryResponse post(@NonNull String url, Consumer<JQueryResponse> consumer) {
            return post(url, EMPTY, consumer);
        }

        public static JQueryResponse post(@NonNull String url, String data, Consumer<JQueryResponse> consumer) {
            method(Method.POST);
            return ajax(url, data, consumer);
        }

        public static JQueryResponse ajax(String url, FormData data, Consumer<JQueryResponse> consumer) {
            contentType(ContentType.FORMDATA);
            return ajax(url, data.toString(), consumer);
        }

        public static JQueryResponse ajax(@NonNull String url, String data, Consumer<JQueryResponse> consumer) {
            var https = Boolean.FALSE;
            if (url.toUpperCase().startsWith("HTTPS")) {
                https = Boolean.TRUE;
            }
            if (METHOD.get() == Method.GET && !isBlank(data)) {
                url += url.indexOf("?") < 0 ? "?" : "&" + data;
            }
            if (isBlank(REFERER.get())) {
                REFERER.set("http" + (https ? "s" : "") + "://" + url.split("/")[2]);
            }
            JQueryResponse response = null;
            try {
                response = request(https, url, data);
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
                response = new JQueryResponse(Boolean.FALSE, e.getMessage());
            }
            consumer.accept(response);
            return response;
        }

        private static JQueryResponse request(Boolean https, String urlStr, String data) throws IOException, NoSuchAlgorithmException, KeyManagementException {
            debug(urlStr, data);
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            var url = https ? new URL(null, urlStr, new Handler()) : new URL(urlStr);
            var conn = url.openConnection();
            JQueryResponse result = null;
            String redirect = null;
            if (https) {
                var sslContext = SSLContext.getInstance("TLSV1.2");
                X509TrustManager[] tm = {new SSLTrustManager()};
                sslContext.init(null, tm, new SecureRandom());
                var httpsConn = ((HttpsURLConnection) conn);
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConn.setRequestMethod(METHOD.get().toString());
                result = URLConnectionRequest(httpsConn, data);
                httpsConn.getHeaderFields();
                redirect = httpsConn.getHeaderField("Location");
                result.setHeaders(httpsConn.getHeaderFields());
                httpsConn.disconnect();
            } else {
                var httpConn = (HttpURLConnection) conn;
                httpConn.setRequestMethod(METHOD.get().toString());
                result = URLConnectionRequest(httpConn, data);
                result.setHeaders(httpConn.getHeaderFields());
                redirect = httpConn.getHeaderField("Location");
                httpConn.disconnect();
            }
            return redirect == null ? result : request(https, redirect, data);
        }

        private static JQueryResponse URLConnectionRequest(final URLConnection conn, String data) throws IOException {
            conn.setConnectTimeout(TIMEOUT.get());
            conn.setRequestProperty("User-Agent", USER_AGENT.get());
            conn.setRequestProperty("Referer", REFERER.get());
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Charset", CHARSET.get());
            if (COOKIE.get().size() > 0) {
                conn.setRequestProperty("Cookie", COOKIE.get().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("; ")));
            }
            HEADER.get().forEach(conn::setRequestProperty);
            conn.setUseCaches(false);
            conn.setReadTimeout(TIMEOUT.get());
            if (METHOD.get() != Method.GET) {
                conn.setRequestProperty("Content-Type", CONTENT_TYPE.get().meta + ";charset=" + CHARSET.get());
                conn.setDoOutput(true);
                conn.setDoInput(true);
                if (!isBlank(data)) {
                    try (OutputStream outputStream = conn.getOutputStream()) {
                        outputStream.write(data.getBytes(CHARSET.get()));
                    }
                }
            } else {
                conn.setRequestProperty("Content-Type", "text/html,application/xml,application/json,text/json,image/png,image/jpeg,*/*;charset=" + CHARSET.get());
            }
            StringBuilder sb = new StringBuilder();
            try (InputStream inStream = conn.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inStream, CHARSET.get());
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                var str = EMPTY;
                while ((str = bufferedReader.readLine()) != null)
                    sb.append(str).append(System.lineSeparator());
                return new JQueryResponse(Boolean.TRUE, sb.toString());
            }
        }

        public static void charset(String charset) {
            CHARSET.set(charset);
        }

        public static void useragent(String useragent) {
            USER_AGENT.set(useragent);
        }

        public static void referer(String referer) {
            REFERER.set(referer);
        }

        public static void timeout(Integer timeout) {
            TIMEOUT.set(timeout);
        }

        public static void method(Method method) {
            METHOD.set(method);
        }

        public static void contentType(ContentType contentType) {
            CONTENT_TYPE.set(contentType);
        }

        public static void header(String name, String value) {
            HEADER.get().put(name, value);
        }

        public static void header(Map<String, String> header) {
            HEADER.get().putAll(header);
        }

        public static void cookie(String name, String value) {
            COOKIE.get().put(name, value);
        }

        public static void cookie(Map<String, String> cookie) {
            COOKIE.get().putAll(cookie);
        }

        public static void cookie(String cookieStr) {
            if (!isBlank(cookieStr)) {
                Arrays.asList(cookieStr.split(";")).forEach(ck -> {
                    var cookieKV = ck.trim().split("=");
                    COOKIE.get().put(cookieKV[0], cookieKV[1]);
                });
            }
        }

        public static Map<String, String> cookie() {
            return COOKIE.get();
        }

        private static void debug(String url, String data) {
            log.info("\n\n\n>>>>>>  URL:\t\t\t{}\n>>>>>>  Method:\t\t\t{}\n>>>>>>  Param:\t\t\t{}\n>>>>>>  Cookie:\t\t\t{}\n>>>>>>  ContentType:\t{}\n>>>>>>  UserAgent:\t\t{}\n>>>>>>  Referer:\t\t{}\n>>>>>>  Timeout:\t\t{}\n>>>>>>  Header:\t\t\t{}\n\n", url, METHOD.get(), data, COOKIE.get(), CONTENT_TYPE.get().meta, USER_AGENT.get(), REFERER.get(), TIMEOUT.get(), HEADER.get());
        }
    }

    public static FormData data(String key, Object val) {
        return new FormData().append(key, val);
    }

    public static FormData data(String k1, Object v1, String k2, Object v2) {
        return new FormData().append(k1, v1, k2, v2);
    }

    public static FormData data(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return new FormData().append(k1, v1, k2, v2, k3, v3);
    }

    public static FormData data(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        return new FormData().append(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static FormData data(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
        return new FormData().append(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static FormData data(Map<String, Object> data) {
        return new FormData().append(data);
    }

    public static class FormData {

        private List<Data> datas = new ArrayList<>();

        public FormData append(String key, Object val) {
            if (key != null && key.length() > 0)
                datas.add(new Data(key, val));
            return this;
        }

        public FormData append(String k1, Object v1, String k2, Object v2) {
            return append(k1, v1).append(k2, v2);
        }

        public FormData append(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return append(k1, v1, k2, v2).append(k3, v3);
        }

        public FormData append(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
            return append(k1, v1, k2, v2, k3, v3).append(k4, v4);
        }

        public FormData append(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
            return append(k1, v1, k2, v2, k3, v3, k4, v4).append(k5, v5);
        }

        public FormData append(Map<String, Object> data) {
            data.entrySet().forEach(entry -> append(entry.getKey(), entry.getKey()));
            return this;
        }

        public String toString() {
            return datas.size() > 0 ? datas.stream().map(Data::toString).collect(Collectors.joining("&")) : JQuery.EMPTY;
        }

        public FormData then(Consumer<FormData> consumer){
            consumer.accept(this);
            return this;
        }

        @AllArgsConstructor
        public class Data {
            private String key;
            private Object val;

            public String toString() {
                return key + "=" + (val == null ? "" : val.toString());
            }
        }
    }

    private static class SSLTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s){

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s){

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
