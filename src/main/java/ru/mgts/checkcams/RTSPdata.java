package ru.mgts.checkcams;

import java.util.List;

/**
 * Created by Dummy on 10.03.2017.
 */
public class RTSPdata
{
    String type;
    String login;
    String pass;
    String port;
    String codec;
    Boolean ispvn;
    List<String> channels;

    public RTSPdata(String type, String login, String pass, String port, String codec, Boolean ispvn, List<String> channels)
    {
        this.type = type;
        this.login = login;
        this.pass = pass;
        this.port = port;
        this.codec = codec;
        this.ispvn = ispvn;
        this.channels = channels;
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

    public Boolean getIspvn()
    {
        return ispvn;
    }

    public List<String> getChannels()
    {
        return channels;
    }
}
