package com.microsoft.aad.msal4j;

public enum Prompt {
    LOGIN ("login"),
    SELECT_ACCOUNT ("select_account"),
    CONSENT ("consent"),
    ADMING_CONSENT ("admin_consent");

    private String prompt;

    Prompt(String prompt){
        this.prompt = prompt;
    }

    @Override
    public String toString(){
        return prompt;
    }
}
