# cas_wx
# 微信和CAS登录整合
<http://lzfblog.sinaapp.com/?p=19>
##一、简介
最近微信连通一切的口号这么火，突然想研究微信认证能否和CAS整合。<br/>
环境：<br/>
微信 6.1 Android版<br/>
CAS  3.4.12 手头的老版本<br/>
##二、微信认证简介
微信授权大致和Oauth协议差不多，主要步奏为：<br/>
源自：http://mp.weixin.qq.com/wiki/17/c0f37d5704f0b64713d5d2c37b468d75.html<br/>
第一步：用户同意授权，获取code<br/>
在确保微信公众账号拥有授权作用域（scope参数）的权限的前提下（服务号获得高级接口后，默认拥有scope参数中的snsapi_base和snsapi_userinfo），引导关注者打开如下页面：<br/>

https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect<br/>
上述snsapi_base模式是静默模式，不需微信用户手动授权，用户感受不到界面的跳转，但仅可获取用户的openid信息，对于我们来说openid暂时足够了。<br/>
第二步：通过code换取网页授权access_token<br/>
获取code后，请求以下链接获取access_token： 
https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
正确时返回的JSON数据包如下：<br/>

{<br/>
   "access_token":"ACCESS_TOKEN",<br/>
   "expires_in":7200,<br/>
   "refresh_token":"REFRESH_TOKEN",<br/>
   "openid":"OPENID",<br/>
   "scope":"SCOPE",<br/>
   "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"<br/>
}<br/>
微信为每个用户，每个公共平台分配了唯一的openid，我们主要目的是为了获取微信用户的openid将它与CAS数据库用户绑定。<br/>
##三、微信和CAS整合
CAS的认证流程是基于Spring mvc 与Spring web-flow，主要流程控制文件为login-webflow.xml，考虑到和微信整合，用户的登录流程大致可以简化为： <br/>
为了稳妥起见，所有关于CAS读写票据的操作，都采用CAS原有流程。<br/>
![image](https://github.com/zhaofali/cas_wx/blob/master/1.png)
详情见博客链接


