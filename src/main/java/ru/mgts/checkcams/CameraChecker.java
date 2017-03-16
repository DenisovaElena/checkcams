package ru.mgts.checkcams;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mgts.checkcams.model.CamStatus;
import ru.mgts.checkcams.model.Camera;
import ru.mgts.checkcams.model.RTSPdata;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by MakhrovSS on 15.03.2017.
 */
public class CameraChecker {

    protected static final Logger LOG = LoggerFactory.getLogger(CControl.class);

    private boolean complete;
    private int camsTestedCount;

    private static ThreadFactory threadFactoryMediaPlayers = new ThreadFactoryBuilder()
            .setNameFormat("mediaPlayers-%d")
            .build();
    protected static ExecutorService serviceMediaPlayer = Executors.newFixedThreadPool(1, threadFactoryMediaPlayers);

    private ThreadFactory threadFactoryCamsTest = new ThreadFactoryBuilder()
            .setNameFormat("camsTest-%d")
            .build();
    private ExecutorService serviceCamsTest = Executors.newFixedThreadPool(10, threadFactoryCamsTest);

    public static Map<String, RTSPdata> rtspDataList = Configurator.loadConfigs();


    public void startCameraIterator(String sourcePath, String destinationPath, String screensPath, LocalTime startTime, LocalTime endTime) {
        try {
            //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
            //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer, 3);

            // инициализируем хэш-карту - классификатор типов камер. Ключ - тип камеры, значение - структура ru.mgts.checkcams.model.RTSPdata

            complete = false;
            camsTestedCount = 0;
            HSSFWorkbook myExcelBook = new HSSFWorkbook(new FileInputStream(sourcePath));
            HSSFSheet sheet = myExcelBook.getSheetAt(0);
            int statusCellNumber = 51;
            sheet.getRow(1).createCell(statusCellNumber).setCellValue("Скрин");
            boolean nameExists = true;
            int currentRow = 2;
            List<CamStatus> resultList = new ArrayList<>();
            while (nameExists && !isComplete()) {
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

                    resultList.add(new CamStatus(serviceCamsTest.submit(new TaskTestCamera(camera, screensPath, startTime, endTime)), cellNetStatus));
                } catch (Exception e) {
                    LOG.info(e.getMessage());
                }
            }


            while (!resultList.isEmpty()) {
                Iterator<CamStatus> iterator = resultList.iterator();
                while(iterator.hasNext()) {
                    CamStatus camStatus = iterator.next();
                    try {
                        if (camStatus.getTask().isDone()) {
                            if (camStatus.getTask().get()) {
                                camStatus.getCellNetStatus().setCellValue("Да");
                                camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.GREEN.index);
                            } else {
                                camStatus.getCellNetStatus().setCellValue("Нет");
                                camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.RED.index);
                            }
                            camsTestedCount++;
                            iterator.remove();
                        }
                    } catch (Exception e) {
                        LOG.info(e.getMessage());
                        camStatus.getCellNetStatus().setCellValue("Нет");
                        camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.RED.index);
                        iterator.remove();
                        camsTestedCount++;
                    }
                }
            }


            boolean writed = true;
            while (writed) {
                try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
                    myExcelBook.write(outputStream);
                    writed = false;
                } catch (IOException e) {
                    LOG.info(e.getMessage());
                }
            }

            myExcelBook.close();
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
        serviceCamsTest.shutdown();
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getCamsTestedCount() {
        return camsTestedCount;
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
