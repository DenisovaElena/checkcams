/**
 * Created by Dummy on 08.03.2017.
 */
public class Camera
{
    String name;
    String ipAddress;
    String type;

    public Camera(String name, String ipAddress, String type)
    {
        this.name = name;
        this.ipAddress = ipAddress;
        this.type = type;
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
}
