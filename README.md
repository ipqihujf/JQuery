针对`java.net.URLConnection`的简单封装，让你在java中使用类似于**jQuery**的代码风格进行网络请求

使用方式，引包：

~~~java
//先静态映入包
//类似于js中<script src="xxx/xxx/xxx/jquery.js"></script>
import static io.github.ydq.jquery.JQuery.*;


public class JQueryTest {

    public static void main(String[] args){
        //最简单的get请求
        JQueryResponse resp = $.get("http://www.xxx.xx/xxx");
        //请求状态
        if (resp.getStatus()) {
            System.out.println("response body:" + resp.getContent());
            System.out.println("response headers:" + resp.getHeaders());
            System.out.println("response cookie:" + resp.cookie());
        } else {
            System.out.println("error message:" + resp.getContent());
        }
        
        //使用lambda方式
        $.get("http://www.xxx.xx/xxx",resp -> {
            if (resp.getStatus()) {
                System.out.println("response body:" + resp.getContent());
                System.out.println("response headers:" + resp.getHeaders());
                System.out.println("response cookie:" + resp.cookie());
            } else {
                System.out.println("error message:" + resp.getContent());
            }
        });
        
		//可以设置一些请求参数 (作用范围是ThreadLocal)
        $.cookie("session=abcdefg; username=admin");
        $.header("token","foobar");
        $.referer("https://www.kongzhongjr.com");
        $.useragent("balabalabala");
        $.timeout(5000);
        $.charset("UTF-8");
        //可以使用data(k,v)以及重载来添加请求参数，使用FormData时contentType默认为FORMDATA
        $.post("https://www.abcdexxx.xxx/login",
				data("username","admin","password","123456")//默认contentType为FORMDATA
				.append("extinfo","balabala"),//当data方法的重载不够用时可以继续append
				resp->{
                    if (resp.getStatus()) {
                        System.out.println("response body:" + resp.getContent());
                        System.out.println("response headers:" + resp.getHeaders());
                        System.out.println("response cookie:" + resp.cookie());
                    } else {
                        System.out.println("error message:" + resp.getContent());
                    }
        		}
		);
        
        //请求参数不仅可以用FormData，还可以直接String
		//非Get请求使用Sring时可以指定contentType (作用范围是ThreadLocal)
        $.contentType(ContentType.JSON);
		$.post("http://www.xxx.xx/xxx","{k1:val1,k2:val2}");
        $.contentType(ContentType.XML);
		$.post("http://www.xxx.xx/xxx","<xml></xml>");
        
        //可以指定请求方法 (作用范围是ThreadLocal)
        $.method(Method.DELETE);//GET, POST, PUT, DELETE, HEAD
        $.ajax("http://www.xxx.xx/xxx")
    }
}
~~~



嗯，大概就是这个样子