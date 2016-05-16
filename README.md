### 目的
开发SafeWebView是因为`@JavascriptInterface`在Api 17之后才提供,在Api 17之前都存在安全性风险.  
(javascript可以通过WebView添加的Java bridge对象,反射取到Runtime,然后执行shell命令等  
不受native app控制的操作, 具体的例子参见: [Android webview(三) addJavascriptInterface的安全问题](http://my.oschina.net/fengheju/blog/673629))

### 实现方式
通过js的prompt()方法以及WebChromeClient的onJsPrompt()回调进行通信.

### 使用方式的对比
* Android 端:
	1. 使用 `@JavascriptInvoker` 代替 `@JavascriptInterface`
	2. 使用 `addJavascriptInvoker` 代替 `addJavascriptInterface`

* Js 端:
	1. 使用`prompt('JsInvoker::JavaIdentifier.method()')`代替 `JavaIdentifier.method()`


例如 提供一个获得 CPU ABI 的方法

* 使用`WebView.addJavascriptInterface()`方法实现: (<font color=red>原先</font>)

```java
// Android 端
public class JavaBridge {
	@JavascriptInterface
	public String getCpuAbi() {
		return Build.CPU_ABI;
	}
}
webView.addJavascriptInterface(new JavaBridge(), "JavaBridge");

//js 端
<script type="text/javascript">
	var cpuAbi = JavaBridge.getCpuAbi();
</script>
```

* 使用 `SafeWebView.addJavascriptInvoker()` 方法实现: (<font color=red>现在</font>)

```java
// Android 端
public class JavaBridge {
	@JavascriptInvoker
	public String getCpuAbi() {
		return Build.CPU_ABI;
	}
}
webView.addJavascriptInvoker(new JavaBridge(), "JavaBridge");

//js 端
<script type="text/javascript">
	var cpuAbi = prompt('JsInvoker::JavaBridge.getCpuAbi()');
</script>
```

### 注意点

##### 如何区分java方法调用和真正的prompt调用
既然是通过prompt()方法去调用java方法,那么就需要区分什么时候是要调用java方法,什么时候是要真正的prompt. `SafeWebView`使用的方式是: prompt方法的参数有`JsInvoker::`前缀的是java方法调用.

* 正常的js prompt的调用方式:

```javascript
var value = prompt('how old are you?');
```

* 调用java方法的prompt:(要添加`JsInvoker::`前缀)

```javascript
var valueFromJava = prompt('JsInvoker::JavaIdentifier.method()');
```

##### js如何向java传递参数
* 调用格式:

```javascript
//1. Android 端提供的identifier: JavaIdentifier
//2. 要调用的方法名: method
//3. 有3个参数: param1, param2, param3
prompt('JsInvoker::JavaIdentifier.method(param1, param2, param3)');
```

* 参数注意事项:
	* 所有参数都必需是 String 类型
	* js调用中,字符串不需要加 单引号`'`和双引号`"`, 以`,`分隔
	
	```javascript
	例如:
	// Java 得到2个参数:
	//param1
	//param2
	prompt("JsInvoker::Android.getData(param1, param2)");

	// Java 得到1个参数(包括单引号和空格):
	//'"par   am
	prompt("JsInvoker::Android.getData('"par   am)");
	```
	
	* 逗号`,`,右括号`)`,反斜线`\`需要转义: `\,`,`\)`,`\\`

	```javascript
	//Java 得到1个参数:
	//par,a)m
	prompt("JsInvoker::Android.getData(par\,a\)m)");

	//Java 得到2个参数:
	//par
	//a)m
	prompt("JsInvoker::Android.getData(par,a\)m)");
	
	//语法错误
	prompt("JsInvoker::Android.getData(par,a)m)");
	```
	
	* 其他需要转义的有: `\n`, `\r`, `\b`, `\f`, `\t`
	* 不支持以 `\0`开头的八进制和`\u`开头的Unicode字符(可以通过添加一个`\`来将其转为一个普通字符串)
	* 不符合上述约束时,将产生一个语法错误(Java层会捕获这个错误,不会崩溃)

##### java方法注意点
1. 提供给js的java方法都必须添加`@JavascriptInvoker`
2. 所有通过`@JavascriptInvoker`提供给js的java方法都必须是`public`的,并且要么不需要参数,要么所有参数都是`String`, 否则该方法不会提供给js.
3. 可以通过`debugCompile`来依赖`buildcheck`模块,它会在编译时检查`@JavascriptInvoker`对应的方法是否合法,如果不合法,会产生一个编译错误.

### 项目模块
* library: 提供了SafeWebView和JavascriptInvoker, 也是其他项目需要依赖的部分
* buildcheck: 提供编译时的方法检查(建议通过 debugCompile 依赖)
* sample: 提供一个使用的例子