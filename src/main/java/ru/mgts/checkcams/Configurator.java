package ru.mgts.checkcams;

import ru.mgts.checkcams.model.RTSPdata;

import java.util.*;

/**
 * Created by Dummy on 10.03.2017.
 */
public class Configurator
{
    public static Map<String, RTSPdata> loadConfigs()
    {
        Map<String, RTSPdata> rtspDataList = new HashMap<String, RTSPdata>();

        // создаем экземпляр класса манимулятора свойств Properties,
        // через который будем манипулировать свойствами
        Properties prop = new Properties();

        try
        {
            // загружаем в манипулятор свойств prop данные из файла со свойствами
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            prop.load(classLoader.getResourceAsStream("rtsp.properties"));
            // загружаем кол-во типов камер
            int count = Integer.parseInt(prop.getProperty("count"));

            for (int i = 0; i < count; i++)
            {
                if (prop.getProperty("type" + i) == null)
                    continue;
                rtspDataList.put(prop.getProperty("type" + i),
                        new RTSPdata(
                                prop.getProperty("type" + i),
                                prop.getProperty("login" + i),
                                prop.getProperty("pass" + i),
                                prop.getProperty("port" + i),
                                prop.getProperty("codec" + i),
                                prop.getProperty("class" + i),
                                prop.getProperty("foldername" + i),
                                prop.getProperty("channel" + i)
                                ));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return rtspDataList;
    }
}