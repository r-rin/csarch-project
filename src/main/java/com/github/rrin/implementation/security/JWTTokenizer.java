package com.github.rrin.implementation.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import java.util.Date;

public class JWTTokenizer {

    // It's not that secret, yeah?
    private final static String SECRET_KEY = System.getenv("SECRET_KEY_SCARCH");

    // Must be 5 hours
    private final static int EXPIRATION_TIME = 1000*60*5;

    public static String getToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(SECRET_KEY));
    }

    public static String verifyToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC512(SECRET_KEY))
                .build()
                .verify(token).getSubject();
    }
}
