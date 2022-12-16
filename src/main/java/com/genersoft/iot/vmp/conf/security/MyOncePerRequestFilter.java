package com.genersoft.iot.vmp.conf.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.utils.JwtUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Author Jiazhenen
 * @Description TODO
 * @Date 2022-12-16 14:42
 * @Version 1.0
 **/
@Component

public class MyOncePerRequestFilter extends OncePerRequestFilter {

    @Value("${jwt.tokenHeader}")
    private String header;
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        // header的值是在yml文件中定义的 “Authorization”
        String token = request.getHeader(header);
        System.out.println("MyOncePerRequestFilter-token = " + token);
        Map<String, Object> map = new HashMap<>();
        map.put("code","0");
        map.put("msg","登陆失败");
        if (token != null && !"".equals(token)) {
            String username = null;
            try {
                token = token.replaceFirst(tokenHead,"");
                Claims claims = JwtUtil.parseJWT(token);
                username = claims.getSubject();
            } catch (Exception e) {
                e.printStackTrace();
                map.put("msg","非法Token，请重新登陆");
                WriteJSON(request,response,map);
                return;
            }
            String redisToken =  Objects.requireNonNull(RedisUtil.get("Token_" + username)).toString();
            if (redisToken == null) {
                WriteJSON(request,response,map);
                return;
            }

            //对比前端发送请求携带的的token是否与redis中存储的一致
            if (!Objects.isNull(redisToken) && redisToken.equals(token)) {
                String s = RedisUtil.get("UserDetails_" + username).toString();
                JSONObject jsonObject = JSON.parseObject(s);
                String user = jsonObject.getString("user");
                String loginTime = jsonObject.getString("loginTime");
                User user1 = JSON.parseObject(user, User.class);
                LocalDateTime loginTime1 = JSON.parseObject(loginTime, LocalDateTime.class);
                LoginUser authUser = new LoginUser(user1,loginTime1);
                if (Objects.isNull(authUser)) {
                    map.put("msg","用户未登陆");
                    WriteJSON(request,response,map);
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authUser, null, authUser.getAuthorities());
                Authentication authenticate = authenticationManager.authenticate(authenticationToken);
                SecurityContextHolder.getContext().setAuthentication(authenticate);
            }
        }
        filterChain.doFilter(request, response);
    }
    private void WriteJSON(HttpServletRequest request,
                           HttpServletResponse response,
                           Object obj) throws IOException, ServletException {
        //这里很重要，否则页面获取不到正常的JSON数据集
        response.setContentType("application/json;charset=UTF-8");

        //跨域设置
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Method", "POST,GET");
        //输出JSON
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(obj));
        out.flush();
        out.close();
    }
}
