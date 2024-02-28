package com.didan.azure.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {
    private static Logger logger = LoggerFactory.getLogger(JwtUtils.class);


    @Value("${jwt.secretkey}")
    private String secretKey;
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // Thời gian hết hạn của access token (1 ngày)

    // Mã hóa data, userId thành accessToken dùng để xác thực người dùng
    public String generateAccessToken(String data){
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        String accessToken = Jwts.builder().signWith(key).subject(data).compact();
        return accessToken;
    }

    // Lấy token từ header
    public String getTokenFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization"); // Lấy token từ header
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){ // Kiểm tra bearerToken khác null và có bắt đầu bằng Bearer
            return bearerToken.substring(7); // Trả về token
        } else return null;
    }

    // Giải mã accessToken để lấy userId
    public String getUserIdFromAccessToken(String accessToken){
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey)); // Giải mã secretKey
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload(); // Giải mã accessToken để lấy payload (data) trong accessToken
        return claims.getSubject().toString(); // Lấy key subject trong payload để trả về email
    }

    // Xác thực accessToken
    public void validateAccessToken(String accessToken) throws Exception {
        try{
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken); // Giải mã accessToken
        }catch(MalformedJwtException e){ // Nếu access token không hợp lệ thì bắn lỗi
            logger.error("Invalid access token");
            throw new Exception("Invalid access token");
        }catch(ExpiredJwtException e) { // Nếu access token hết hạn thì bắn lỗi
            logger.error("Expired access token");
            throw new Exception("Expired access token");
        }catch(UnsupportedJwtException e){ // Nếu access token không được hỗ trợ thì bắn lỗi
            logger.error("Unsupported access token");
            throw new Exception("Unsupported access token");
        }catch(IllegalArgumentException e){ // Nếu không có thông tin trong access token thì bắn lỗi
            logger.error("Empty access token");
            throw new Exception("Empty access token");
        }
    }
}