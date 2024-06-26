package org.brito.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UsuarioCredenciais {

    @JsonProperty("login")
    private String login;

    @JsonProperty("password")
    private String password;

    public UsuarioCredenciais(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public UsuarioCredenciais() {
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
