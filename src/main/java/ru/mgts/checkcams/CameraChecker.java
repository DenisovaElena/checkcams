package ru.mgts.checkcams;

import com.sun.jna.NativeLibrary;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mgts.checkcams.model.Camera;
import ru.mgts.checkcams.model.RTSPdata;
import ru.mgts.checkcams.util.DateTimeUtil;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Created by MakhrovSS on 15.03.2017.
 */
public class CameraChecker {

    private boolean complete;
    private int camsTestedCount;

    protected static final Logger LOG = LoggerFactory.getLogger(CControl.class);

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getCamsTestedCount() {
        return camsTestedCount;
    }

    public void startCameraIterator(String sourcePath, String destinationPath, String screensPath, LocalTime startTime, LocalTime endTime)
    {
        try
        {
            complete = false;
            camsTestedCount = 0;
            MediaPlayer mediaPlayer = initMediaPlayer();

            //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer, 3);

            // инициализируем хэш-карту - классификатор типов камер. Ключ - тип камеры, значение - структура ru.mgts.checkcams.model.RTSPdata
            Map<String, RTSPdata> rtspDataList = Configurator.loadConfigs();

            String destinationFile = "C:\\resultStatusCams.xls";

            HSSFWorkbook myExcelBook = new HSSFWorkbook(new FileInputStream(sourcePath));
            HSSFSheet sheet = myExcelBook.getSheetAt(0);
            int statusCellNumber = 51;
            sheet.getRow(1).createCell(statusCellNumber).setCellValue("Скрин");
            boolean nameExists = true;
            int currentRow = 2;
            while (nameExists && !isComplete()) {
                //while (LocalTime.now().isBefore(startTime) && LocalTime.now().isAfter(endTime))
                //{
                //    Thread.sleep(1000);
                //}
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
                    if (!rtspData.isPvn()) {
                        rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                                rtspData.getLogin(), rtspData.getPass(),
                                camera.getIpAddress(), rtspData.getPort(),
                                rtspData.getChannel());

                        // маска имени файла, начиная с папки. Разеделние на папки через /
                        screenNameMask =
                                screensPath +
                                        "/" + getNowDate() +
                                        "/" + "DVN-MMS" +
                                        "/" + camera.getName() + "_IP" + camera.getIpAddress() + ".png";
                    } else {
                        String channel = camera.getCamPort().equals("1") ?
                                rtspData.getChannel().replace("[PORT]", "") :
                                rtspData.getChannel().replace("[PORT]", camera.getCamPort());
                        rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                                rtspData.getLogin(), rtspData.getPass(),
                                camera.getIpAddress(), rtspData.getPort(),
                                channel);

                        // маска имени файла, начиная с папки. Разеделние на папки через /
                        screenNameMask =
                                screensPath +
                                        "/" + getNowDate() +
                                        "/" + "PVN" +
                                        "/" + "IP" + camera.getIpAddress() +
                                        "/" + camera.getName() + ".png";
                    }

                    camStatus = saveScreen(rtspAddress, screenNameMask, mediaPlayer, 5);
                }
                catch (Exception e)
                {
                    LOG.debug(e.getMessage());
                }
                finally {
                    if (!nameExists) {
                        break;
                    }
                    else
                    {
                        camsTestedCount++;
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
                        try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
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
        complete = true;
    }

    private String getNowDate()
    {
        return LocalDate.now().format(DateTimeUtil.DATE_FORMATTER);
    }

    private MediaPlayer initMediaPlayer()
    {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\vlc64");
        MediaPlayerFactory factory = new MediaPlayerFactory();
        return factory.newEmbeddedMediaPlayer();
    }

    private boolean saveScreen(final String rtspAddress, final String savePath, final MediaPlayer mediaPlayer, int repeatsCount)
    {
        try {
            LOG.debug("ACHTUNG! Starting vlc for stream " + rtspAddress);
            Thread playThread = new Thread()
            {
                @Override
                public void run() {
                    mediaPlayer.playMedia(rtspAddress);
                }
            };
            playThread.start();
            Thread.sleep(20000);
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
        LOG.debug("ACHTUNG! Player for stream " + rtspAddress + " closed");
        if(!(new File(savePath).exists()) && repeatsCount-- > 0) {
            LOG.debug("ACHTUNG! Not available screen for stream " + rtspAddress + ". Trying again, elapsed repeats: " + repeatsCount);
            saveScreen(rtspAddress, savePath, mediaPlayer, repeatsCount);
        }
        return (new File(savePath).exists());
    }

    private boolean pingHost(String host) {
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
            List<ru.mgts.checkcams.model.Camera> cameraList = readFromExcel("S:\\camertest.xls");

            System.setProperty("webdriver.ie.driver", "C:\\webdriver\\IEDriverServer.exe");
            WebDriver driver = new InternetExplorerDriver();
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.MINUTES);
            driver.manage().window().maximize();



            String login = "", password = "";

            for (int i = 0; i< 1; i++) // вернуть вместо 1 - cameraList.size()
            {
                ru.mgts.checkcams.model.Camera camera = cameraList.get(i);
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
