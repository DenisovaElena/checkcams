package ru.mgts.checkcams;

/**
 * Created by Dummy on 08.03.2017.
 */
public class Camera
{
    String name;
    String ipAddress;
    String type;
    String camPort;

    public Camera(String name, String ipAddress, String type, String camPort)
    {
        this.name = name;
        this.ipAddress = ipAddress;
        this.type = type;
        this.camPort = camPort;
    }

    public String getName()
    {
        return name;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public String getType()
    {
        return type;
    }

    public String getCamPort()
    {
        return camPort;
    }
}
