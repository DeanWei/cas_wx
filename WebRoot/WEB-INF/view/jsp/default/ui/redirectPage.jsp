<html>
  <head>
  </head>
  <body>
  <script>
  function urlencode (str) {  
	    str = (str + '').toString();   
	    return encodeURIComponent(str).replace(/!/g, '%21').replace(/'/g, '%27').replace(/\(/g, '%28').  
	    replace(/\)/g, '%29').replace(/\*/g, '%2A').replace(/%20/g, '+');  
	} 
  var url = "http://service.netentsec.com/cas_debug/login?execution=${flowExecutionKey}&_eventId=code";
  url = urlencode(url);
  //location = url;
  location="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx825fa487322669e0&redirect_uri="+url+"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
  </script>
  </body>
</html>


