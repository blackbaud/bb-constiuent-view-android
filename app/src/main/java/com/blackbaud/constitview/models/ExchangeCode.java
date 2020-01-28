package com.blackbaud.constitview.models;

import android.content.Context;

public class ExchangeCode {
    Context context;
    String code;

    public ExchangeCode(Context _context, String _code) {
        this.context = _context;
        this.code = _code;
    }

    public Context getContext(){
        return this.context;
    }

    public String getCode() {
        return this.code;
    }
}
