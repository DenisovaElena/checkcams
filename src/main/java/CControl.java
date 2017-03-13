/**
 * Created by Dummy on 07.03.2017.
 */

import com.sun.jna.NativeLibrary;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CControl
{
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd_@HH-mm";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static void main(String[] args)
    {
        try
        {
            MediaPlayer mediaPlayer = initMediaPlayer();

            //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer);

            String screensPath = "C:\\screens\\";
            Map<String, RTSPdata> rtspDataList = Configurator.loadConfigs();
            List<Camera> cameraList = readFromExcel("C:\\camertest.xls");
            for (int i = 0; i < cameraList.size(); i++)
            {
                try
                {
                    Camera camera = cameraList.get(i);
                    if (!pingHost(camera.getIpAddress()))
                    {
                        throw new Exception("No ping from camera " +
                                camera.getName() + " with ip " + camera.getIpAddress());
                    }
                    RTSPdata rtspData;
                    if (rtspDataList.containsKey(camera.getType()))
                    {
                        rtspData = rtspDataList.get(camera.getType());
                    } else
                    {
                        throw new Exception("Storage has no type of camera " + camera.getType());
                    }

                    for (int j = 0; j < rtspData.getChannels().size(); j++)
                    {
                        String rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                                rtspData.getLogin(), rtspData.getPass(),
                                camera.getIpAddress(), rtspData.port,
                                rtspData.getChannels().get(j));
                        saveScreen(
                                rtspAddress,
                                screensPath + camera.getName() + "_" + camera.getIpAddress() + "/" +
                                        rtspData.getChannels().get(j).replace("/","-") + getNowDateTime() + ".png",
                                mediaPlayer);
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getNowDateTime()
    {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER).toString();
    }

    public static MediaPlayer initMediaPlayer()
    {
        NativeLibrary.addSearchPath("libvlc", "C:\\vlc");
        MediaPlayerFactory factory = new MediaPlayerFactory();
        return factory.newEmbeddedMediaPlayer();
    }

    public static boolean saveScreen(final String rtspAddress, String savePath, final MediaPlayer mediaPlayer)
    {
        boolean result = false;
        try {

            Thread playThread = new Thread()
            {
                @Override
                public void run() {
                    mediaPlayer.playMedia(rtspAddress);
                }
            };
            playThread.start();
            Thread.sleep(30000);
            File file = new File(savePath);
            mediaPlayer.saveSnapshot(file);
            mediaPlayer.stop();
            playThread.interrupt();
            result = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Player for stream " + rtspAddress + " closed");
        return result;
    }

    public static List<Camera> readFromExcel(String file) throws IOException {
        List<Camera> cameraList = new ArrayList<Camera>();
        HSSFWorkbook myExcelBook = new HSSFWorkbook(new FileInputStream(file));
        HSSFSheet sheet = myExcelBook.getSheetAt(0);

        int rowsCount = sheet.getPhysicalNumberOfRows();

        for(int r = 2; r < rowsCount; r++) {
            try {
                HSSFRow row = sheet.getRow(r);
                HSSFCell cell = row.getCell(2);
                HSSFCell cell2 = row.getCell(8);
                HSSFCell cell3 = row.getCell(7);

                cameraList.add(new Camera(cell.getStringCellValue().trim(), cell2.getStringCellValue().trim(), cell3.getStringCellValue().trim()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("Ошибка чтения строки" + r + "из файла Excel");
            }
        }

        myExcelBook.close();
        return cameraList;
    }

    public static boolean pingHost(String host) {
        try {
            return InetAddress.getByName(host).isReachable(1000);
        } catch (IOException e) {
            return false;
        }
    }

}

            /*
            List<Camera> cameraList = readFromExcel("S:\\camertest.xls");

            System.setProperty("webdriver.ie.driver", "C:\\webdriver\\IEDriverServer.exe");
            WebDriver driver = new InternetExplorerDriver();
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.MINUTES);
            driver.manage().window().maximize();



            String login = "", password = "";

            for (int i = 0; i< 1; i++) // вернуть вместо 1 - cameraList.size()
            {
                Camera camera = cameraList.get(i);
                String link = camera.getIpAddress();
                if (camera.getType().equals("beward_75")){
                    login = "admin";
                    password = "admin";
                }
                else if (camera.getType().equals("rvi ipc50dn12")){
                    login = "admin";
                    password = "admin";
                }


                String URL = "http://" + login + ":" + password + "@" + link;
                driver.get(URL);
                //driver.findElement(By.name("username")).sendKeys("admin@gmail.com");
                //driver.findElement(By.name("password")).sendKeys("admin");
                //driver.findElement(By.className("btn-success")).submit();


                File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(scrFile, new File("S:\\screens\\"+camera.getName()+".png"));
            }
            */

