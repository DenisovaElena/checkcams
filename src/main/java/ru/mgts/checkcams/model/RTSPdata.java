package ru.mgts.checkcams.model;

import java.util.List;

/**
 * Created by Dummy on 10.03.2017.
 */
public class RTSPdata
{
    private String type;
    private String login;
    private String pass;
    private String port;
    private String codec;
    private boolean pvn;
    private String channel;

    public RTSPdata(String type, String login, String pass, String port, String codec, Boolean ispvn, String channel)
    {
        this.type = type;
        this.login = login;
        this.pass = pass;
        this.port = port;
        this.codec = codec;
        this.pvn = ispvn;
        this.channel = channel;
    }

    public String getType()
    {
        return type;
    }

    public String getLogin()
    {
        return login;
    }

    public String getPass()
    {
        return pass;
    }

    public String getPort()
    {
        return port;
    }

    public String getCodec()
    {
        return codec;
    }

    public boolean isPvn() {
        return pvn;
    }

    public String getChannel() {
        return channel;
    }
}
