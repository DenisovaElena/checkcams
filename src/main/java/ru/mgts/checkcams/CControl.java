package ru.mgts.checkcams; /**
 * Created by Dummy on 07.03.2017.
 */

import com.sun.jna.NativeLibrary;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class CControl
{
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    protected static final Logger LOG = LoggerFactory.getLogger(CControl.class);

    public static void main(String[] args)
    {
        try
        {
            LOG.info("test");
            MediaPlayer mediaPlayer = initMediaPlayer();

            //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer);

            String screensPath = "C:\\screens\\";
            // инициализируем хэш-карту - классификатор типов камер. Ключ - тип камеры, значение - структура ru.mgts.checkcams.RTSPdata
            Map<String, RTSPdata> rtspDataList = Configurator.loadConfigs();

            String sourceFile = "C:\\camertest.xls";
            String destinationFile = "C:\\resultStatusCams.xls";

            HSSFWorkbook myExcelBook = new HSSFWorkbook(new FileInputStream(sourceFile));
            HSSFSheet sheet = myExcelBook.getSheetAt(0);
            int statusCellNumber = 51;
            sheet.getRow(1).createCell(statusCellNumber).setCellValue("Скрин");
            boolean nameExists = true;
            int currentRow = 2;
            while (nameExists) {
                boolean camStatus = false;
                HSSFCell cellNetStatus = null;
                try {
                    HSSFRow row = sheet.getRow(currentRow);
                    if (row == null || row.getCell(7) == null || row.getCell(7).toString().trim().equals("")) {
                        nameExists = false;
                        break;
                    }
                    HSSFCell cellName = row.getCell(2); // name
                    HSSFCell cellIpAddress = row.getCell(8); // ipAddress
                    HSSFCell cellType = row.getCell(7); // type
                    HSSFCell cellCamPort = row.getCell(9); // camPort
                    cellNetStatus = row.createCell(statusCellNumber); // netStatus

                    currentRow++;
                    Camera camera = new Camera(cellName.getStringCellValue().trim(),
                            cellIpAddress.getStringCellValue().trim(),
                            cellType.getStringCellValue().trim(),
                            cellCamPort.getStringCellValue().trim()
                    );

                    if (!pingHost(camera.getIpAddress())) {
                        throw new Exception("ACHTUNG! No ping from camera " +
                                camera.getName() + " with ip " + camera.getIpAddress());
                    }
                    RTSPdata rtspData;
                    if (rtspDataList.containsKey(camera.getType())) {
                        rtspData = rtspDataList.get(camera.getType());
                    } else {
                        throw new Exception("ACHTUNG! PropertiesFile has no type of camera " + camera.getType());
                    }

                    String rtspAddress;
                    String screenNameMask;
                    if (!rtspData.ispvn) {
                        rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                                rtspData.getLogin(), rtspData.getPass(),
                                camera.getIpAddress(), rtspData.port,
                                rtspData.getChannel());

                        // маска имени файла, начиная с папки. Разеделние на папки через /
                        screenNameMask =
                                screensPath +
                                        "/" + getNowDate() +
                                        "/" + "DVN-MMS" +
                                        "/" + camera.getName() + "_IP" + camera.getIpAddress() + ".png";
                    } else {
                        String channel = camera.camPort.equals("1") ?
                                rtspData.getChannel().replace("[PORT]", "") :
                                rtspData.getChannel().replace("[PORT]", camera.camPort);
                        rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                                rtspData.getLogin(), rtspData.getPass(),
                                camera.getIpAddress(), rtspData.port,
                                channel);

                        // маска имени файла, начиная с папки. Разеделние на папки через /
                        screenNameMask =
                                screensPath +
                                        "/" + getNowDate() +
                                        "/" + "PVN" +
                                        "/" + "IP" + camera.getIpAddress() +
                                        "/" + camera.getName() + ".png";
                    }

                    camStatus = saveScreen(rtspAddress, screenNameMask, mediaPlayer, 2);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                finally {
                    if (!nameExists) {
                        break;
                    }
                    if (camStatus)
                    {
                        cellNetStatus.setCellValue("Да");
                        cellNetStatus.getCellStyle().setFillForegroundColor(HSSFColor.GREEN.index);
                    }
                    else
                    {
                        cellNetStatus.setCellValue("Нет");
                        cellNetStatus.getCellStyle().setFillForegroundColor(HSSFColor.RED.index);
                    }
                    boolean writed = true;
                    while (writed) {
                        try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                            myExcelBook.write(outputStream);
                            writed = false;
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            myExcelBook.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static String getNowDate()
    {
        return LocalDate.now().format(DATE_FORMATTER).toString();
    }

    public static MediaPlayer initMediaPlayer()
    {
        NativeLibrary.addSearchPath("libvlc", "C:\\vlc");
        MediaPlayerFactory factory = new MediaPlayerFactory();
        return factory.newEmbeddedMediaPlayer();
    }

    public static boolean saveScreen(final String rtspAddress, final String savePath, final MediaPlayer mediaPlayer, int repeatsCount)
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
            Thread.sleep(15000);
            File file = new File(savePath);
            mediaPlayer.saveSnapshot(file);
            mediaPlayer.stop();
            //playThread.interrupt();
            playThread.join();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("ACHTUNG! Player for stream " + rtspAddress + " closed");
        if(!(new File(savePath).exists()) && repeatsCount > 0) {
            repeatsCount--;
            saveScreen(rtspAddress, savePath, mediaPlayer, repeatsCount);
        }
        else
        {
            result = true;
        }
        return result;
    }

    public static boolean pingHost(String host) {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -n 1 " + host);
            int returnVal = p1.waitFor();
            return  (returnVal==0);
        } catch (Exception e) {
            return false;
        }
    }

}

            /*
            List<ru.mgts.checkcams.Camera> cameraList = readFromExcel("S:\\camertest.xls");

            System.setProperty("webdriver.ie.driver", "C:\\webdriver\\IEDriverServer.exe");
            WebDriver driver = new InternetExplorerDriver();
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.MINUTES);
            driver.manage().window().maximize();



            String login = "", password = "";

            for (int i = 0; i< 1; i++) // вернуть вместо 1 - cameraList.size()
            {
                ru.mgts.checkcams.Camera camera = cameraList.get(i);
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

