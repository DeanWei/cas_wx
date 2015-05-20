package com.lzf;

import javax.servlet.http.HttpServletRequest;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.webflow.execution.RequestContext;

public class JudgeWeiXinAction {
	
    /** 微信内置浏览器标志**/
    private final String WEIXINAGENT = "MicroMessenger";
	//private final String WEIXINAGENT = "Chrome";
    
    public JudgeWeiXinAction() {
    	System.out.println("ahahah");
	}
    
	public final String judge(final RequestContext context) {
		final HttpServletRequest request = WebUtils.getHttpServletRequest(context);
		final String userAgent = request.getHeader("User-Agent");
        System.out.println("Request user-agent " + userAgent);
        if(userAgent == null || "".equals(userAgent)){
        	return "NO";
        }
        if(userAgent.contains(WEIXINAGENT)){
        	return "YES";
        }
        return "NO";
    }
}
