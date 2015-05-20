# cas_wx
微信和CAS登录整合
一、简介
最近微信连通一切的口号这么火，突然想研究微信认证能否和CAS整合。
环境：
微信 6.1 Android版
CAS  3.4.12 手头的老版本
二、微信认证简介
微信授权大致和Oauth协议差不多，主要步奏为：
源自：http://mp.weixin.qq.com/wiki/17/c0f37d5704f0b64713d5d2c37b468d75.html
第一步：用户同意授权，获取code
在确保微信公众账号拥有授权作用域（scope参数）的权限的前提下（服务号获得高级接口后，默认拥有scope参数中的snsapi_base和snsapi_userinfo），引导关注者打开如下页面：

https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect
上述snsapi_base模式是静默模式，不需微信用户手动授权，用户感受不到界面的跳转，但仅可获取用户的openid信息，对于我们来说openid暂时足够了。
第二步：通过code换取网页授权access_token
获取code后，请求以下链接获取access_token： 
https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
正确时返回的JSON数据包如下：

{
   "access_token":"ACCESS_TOKEN",
   "expires_in":7200,
   "refresh_token":"REFRESH_TOKEN",
   "openid":"OPENID",
   "scope":"SCOPE",
   "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
}
微信为每个用户，每个公共平台分配了唯一的openid，我们主要目的是为了获取微信用户的openid将它与CAS数据库用户绑定。
三、微信和CAS整合
CAS的认证流程是基于Spring mvc 与Spring web-flow，主要流程控制文件为login-webflow.xml，考虑到和微信整合，用户的登录流程大致可以简化为： 
为了稳妥起见，所有关于CAS读写票据的操作，都采用CAS原有流程。
现对login-webflow.xml修改如下：
<decision-state id="gatewayRequestCheck">
		<if test="externalContext.requestParameterMap['gateway'] neq '' &amp;&amp; externalContext.requestParameterMap['gateway'] neq null &amp;&amp; flowScope.service neq null" then="gatewayServicesManagementCheck" else="judgeWX" />
	</decision-state>
在检测到用户未登录后，先判断用户是否来自微信客户端
添加一个action-state，用户判断请求是否来自微信客户端
<!-- 判断请求是否来自微信服务器 -->
    <action-state id="judgeWX">
        <evaluate expression="judgeWXAction.judge(flowRequestContext)" />
		<transition on="NO" to="viewLoginForm" />
		<transition on="YES" to="viewRedirectPage" />
	</action-state>
在cas-servlet.xml中添加这个自定义的Action
<!-- 判断是否来自微信服务器 -->
	<bean id="judgeWXAction" class="com.lzf.JudgeWeiXinAction"
		 />
该Action主要是通过http请求header中的user-agent是否包含”MicroMessenger”字符串来判断是否来自微信客户端。
如果请求来微信客户端，定义一个view-state用于重定向用户至微信api获取用于微信请求的code
<view-state id="viewRedirectPage" view="redirectPage" >
     <transition on="code" to="getCode">
        </transition>
</view-state>
这时需要在/WEB-INF/view/jsp/default/ui下添加一个redirectPage.jsp，其主要功能就是生成redirect_uri，并控制浏览器跳转


  function urlencode (str) {  
	    str = (str + '').toString();   
	    return encodeURIComponent(str).replace(/!/g, '%21').replace(/'/g, '%27').replace(/\(/g, '%28').  
	    replace(/\)/g, '%29').replace(/\*/g, '%2A').replace(/%20/g, '+');  
	} 
  var url = "http://service.netentsec.com/cas_debug/login?execution=${flowExecutionKey}&_eventId=code";
  url = urlencode(url);
  //location = url;
  location="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx825fa487322669e0&redirect_uri="+url+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";



微信客户端会回调redirect_uri，并携带获取openid的code参数，此时我们将流程跳转至状态getCode
<action-state id="getCode">
        <evaluate expression="getCodeAction.getCode(flowRequestContext)" />
		<transition on="WEIXINNO" to="generateLoginTicket" />
		<transition on="WEIXINOK" to="generateLoginTicket_WX" />
	</action-state>

在cas-servlet.xml中配置getCodeAction
<bean id="getCodeAction" class="com.lzf.GetCodeAction"
		 p:dataSource-ref="dataSource"/>

getCodeAction.getCode方法主要功能就是根据微信api获取微信用户的openid，然后判断cas数据库中是否包含与该openid关联的用户。
如果能找到，则返回WEIXINOK字符串，此时将微信用户重定向至一个自定义的登录界面，并填写好用户名和openid，自动提交表单，完成登录，为了达到这个效果，需自定义一个登陆界面casLoginView_WX.jsp，将原有登录界面的用户名，密码自动填写，并在页面尾部添加自动提交的js代码。为了达到这个效果需自定义QueryDatabaseAuthenticationHandler类或添加两个QueryDatabaseAuthenticationHandler，并将其中一个的sql语句改为select openid from t_user where username = ?，用以验证openid与username是否相关联。
主要代码为：
String username = jdbcTemplate.queryForObject(sql, String.class, openid);
		        if(username != null && !"".equals(username)){
		        	//若存在与openid相关联的用户，则已openid为密码，自动登录
		        	request.setAttribute("username", username);
		        	request.setAttribute("openid", openid);
		        	return "WEIXINOK";
		        }else{
		        	//若没有与openid相关联的用户，则将openid存储到flowscope中
		        	context.getFlowScope().put("openid", openid);
		        }
如果不能找到，则返回WEIXINNO字符串，则将openid存入flowscope
context.getFlowScope().put("openid", openid);然后用户进入正常的登录流程，在验证用户密码成功后，发放TGT之前，即状态realSubmit，修改AuthenticationViaFormAction的submit方法，将存在flowscope中的openid存入数据库中
//将openid存入数据库中
        String openid = context.getFlowScope().getString("openid");
        System.out.println("get openid from flow scope"+openid);
        if(openid != null){
        	jdbcTemplate = new SimpleJdbcTemplate(dataSource);
        	UsernamePasswordCredentials credentials_ = (UsernamePasswordCredentials) credentials;
        	String username = credentials_.getUsername();
        	String sql = "update t_user set openid = ? where username = ?";
        	jdbcTemplate.update(sql, openid,username);
        }
至此，相当于将微信用户的openid绑定了cas数据库的用户名，用户下次登录时就可以自动提交登录了。

