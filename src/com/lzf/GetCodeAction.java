package com.lzf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.jasig.cas.web.support.WebUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.webflow.execution.RequestContext;

public class GetCodeAction {
	
	private final String AppID = "wx825fa487322669e0";
	private final String AppSecret = "101de81979cac9d8098fba2219608278";
    private final String TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?";
    
    private DataSource dataSource;
    private SimpleJdbcTemplate jdbcTemplate;
    
	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public final String getCode(final RequestContext context) {
		final HttpServletRequest request = WebUtils.getHttpServletRequest(context);
        String code = request.getParameter("code");
        System.out.println("code:"+request.getParameter("code"));
        
        String surl = TOKEN_URL+"appid="+AppID+"&secret="+AppSecret+"&code="+code+"&grant_type=authorization_code";
        URL url = null;
        HttpURLConnection con = null;
        BufferedReader reader = null;
        try{
	        url = new URL(surl);
	        con = (HttpURLConnection) url.openConnection();
	        con.setRequestMethod("GET");
	        con.setConnectTimeout(6000);
	        
	        reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        StringBuffer sb = new StringBuffer();
	        String line = null;
	        while((line = reader.readLine()) != null){
	        	sb.append(line);
	        }
	        
	        Gson gson = new Gson();
	        Response resJson = gson.fromJson(sb.toString(), Response.class);
	        
	        String openid = resJson.getOpenid();
	        
	        System.out.println("openid:"+openid);
	        
	        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	        String sql = "select username from t_user where openid = ?";
	        try{
		        String username = jdbcTemplate.queryForObject(sql, String.class, openid);
		        if(username != null && !"".equals(username)){
		        	//��������openid��������û�������openidΪ���룬�Զ���¼
		        	request.setAttribute("username", username);
		        	request.setAttribute("openid", openid);
		        	return "WEIXINOK";
		        }else{
		        	//��û����openid��������û�����openid�洢��flowscope��
		        	context.getFlowScope().put("openid", openid);
		        }
	        }catch (Exception e){
	        	e.printStackTrace();
	        	//��û����openid��������û�����openid�洢��flowscope��
	        	context.getFlowScope().put("openid", openid);
	        	System.out.println("put openid:"+context.getFlowScope().getString("openid"));
	        }
        }catch (Exception e){
        	e.printStackTrace();
        	return "WEIXINNO";
        }finally{
        	if(con != null)
        		con.disconnect();
        	if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        }
        return "WEIXINNO";
    }
}
