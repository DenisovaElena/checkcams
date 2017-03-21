package ru.mgts.checkcams.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 21.03.2017.
 */
public class Test {
    public static void main(String[] args) {
        int size = 0;
        int maxCamsPerDay = 1000;
        int engineersCountPerDay = 6;
        int camsPerEngineer = maxCamsPerDay / engineersCountPerDay;
        int remainder = maxCamsPerDay % engineersCountPerDay;

        int currentEngineer = 1;
        Map<Integer, Integer> engineerList = new HashMap<>();
        int addRemainder = remainder == 0 ? 0 : 1;
        for (int i = 0; i < maxCamsPerDay; i++)
        {
            if (engineerList.containsKey(currentEngineer))
            {
                engineerList.put(currentEngineer, (engineerList.get(currentEngineer)) + 1);
            }
            else
            {
                engineerList.put(currentEngineer, 1);
            }

            if ((i + 1) % (camsPerEngineer + addRemainder) == 0 && currentEngineer < engineersCountPerDay) {
                currentEngineer++;
                if (remainder !=0)
                {
                    remainder--;
                    addRemainder = 1;
                }
                else
                {
                    addRemainder = 0;
                }
            }
        }
        for (Map.Entry<Integer, Integer> entry : engineerList.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
    }
}
