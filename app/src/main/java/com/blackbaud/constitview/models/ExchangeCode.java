package com.blackbaud.constitview.models;

import android.content.Context;

public class ExchangeCode {
    private Context context;
    private String code;
    private String refreshToken;
    private String grantType;
    private String redirectUri;

    public ExchangeCode(Context _context, String _code, String _refreshToken, String _grantType, String _redirectUri) {
        this.context = _context;
        this.code = _code;
        this.refreshToken = _refreshToken;
        this.grantType = _grantType;
        this.redirectUri = _redirectUri;
    }

    public Context getContext(){ return this.context; }

    public String getCode() { return this.code; }

    public String getRefreshToken() { return this.refreshToken; }

    public String getGrantType() { return this.grantType; }

    public String getRedirectUri() { return this.redirectUri; }
}

