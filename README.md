### JQuery

---

基于`java.net.URLConnection`的轻量简单封装，让你在java中使用类似于**jQuery**的代码风格进行网络请求

> 最低要求java8

##### 引入方式：

maven项目引入依赖：

~~~xml
<dependency>
    <groupId>io.github.ydq</groupId>
    <artifactId>JQuery</artifactId>
    <version>1.1</version>
</dependency>
~~~

##### 使用方法：

下面是一个简单的使用demo：

~~~java
//先静态引入包
//类似于js中<script src="xxx/xxx/xxx/jquery.js"></script>
import static io.github.ydq.jquery.JQuery.*;


public class JQueryTest {

    public static void main(String[] args){
        //最简单的get请求
        JQueryResponse resp = $.get("http://www.xxx.xx/xxx");
        //请求状态
        if (resp.getStatus()) {
            System.out.println("response body:" + resp.content());
            System.out.println("response headers:" + resp.headers());
            System.out.println("response cookie:" + resp.cookie());
        } else {
            System.out.println("error message:" + resp.getContent());
        }
        
        //使用lambda方式
        $.get("http://www.xxx.xx/xxx",resp -> {
            //balabala 为所欲为
        });

        //当然也可以这样
        $.get("http://www.xxx.xx/xxx").then(resp->{
            //balabala 为所欲为
        });
        
        //可以设置一些请求参数 (作用范围是ThreadLocal)
        $.cookie("session=abcdefg; username=admin");    //也可以以键值对的形式传入，具体看重载
        $.header("token","foobar");
        $.referer("http://www.xxx.xx/xxx");             //默认 当前url的domain
        $.useragent("balabalabala");                    //默认 IE11
        $.timeout(5000);                                //默认 5000ms
        $.charset("UTF-8");                             //默认 UTF08

        //可以使用data(k,v)以及重载来添加请求参数，使用FormData时contentType默认为FORMDATA
        $.post("http://www.xxx.xx/xxx",
                data("username","admin","password","123456")//默认contentType为FORMDATA
                .append("extinfo","balabala"),//当data方法的重载不够用时可以继续append追加参数
                resp->{
                    //balabala 为所欲为
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