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
import ru.mgts.checkcams.util.DateTimeUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private volatile boolean complete;
    private int camsTestedCount;
    private int camsTestedTodayCount;

    private String sourcePath;
    private String screensPath;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCamsPerDay;
    private String contractor;
    private int engineersCountPerDay;

    protected static ExecutorService serviceMediaPlayer;
    protected static ExecutorService serviceCamsTest;
    public static Map<String, RTSPdata> rtspDataList = Configurator.loadConfigsRTSP();

    public CameraChecker() {
    }

    public void init(String sourcePath, String screensPath, LocalTime startTime, LocalTime endTime, int maxCamsPerDay, String contractor, int engineersCountPerDay)
    {
        this.sourcePath = sourcePath;
        this.screensPath = screensPath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCamsPerDay = maxCamsPerDay;
        this.contractor = contractor;
        this.engineersCountPerDay = engineersCountPerDay;
    }

    public void reset()
    {
        ThreadFactory threadFactoryMediaPlayers = new ThreadFactoryBuilder()
                .setNameFormat("mediaPlayers-%d")
                .build();
        ThreadFactory threadFactoryCamsTest = new ThreadFactoryBuilder()
                .setNameFormat("camsTest-%d")
                .build();
        serviceMediaPlayer = Executors.newFixedThreadPool(1, threadFactoryMediaPlayers);
        serviceCamsTest = Executors.newFixedThreadPool(10, threadFactoryCamsTest);

    }

    public void startCameraIterator() {
        while (!isComplete()) {
            try (InputStream inputStream = new FileInputStream(sourcePath)) {
                while ((isMaxTestedPerDayLock() || isWorkTimeLock()) && !isComplete()) {
                    Thread.sleep(1000);
                }

                //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
                //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
                //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer, 3);

                reset();
                camsTestedCount = 0;
                camsTestedTodayCount = 0;
                int camsPerEngineer = maxCamsPerDay / engineersCountPerDay;
                int remainder = maxCamsPerDay % engineersCountPerDay;
                HSSFWorkbook myExcelBook = new HSSFWorkbook(inputStream);
                HSSFSheet sheet = myExcelBook.getSheetAt(0);
                int dateNetStatusCellNumber = 51;
                int netStatusCellNumber = 52;
                int engineerNumCellNumber = 53;
                int resultCellNumber = 54;
                sheet.getRow(1).createCell(dateNetStatusCellNumber).setCellValue("Дата опроса");
                sheet.getRow(1).createCell(netStatusCellNumber).setCellValue("Скрин-да/нет");
                sheet.getRow(1).createCell(engineerNumCellNumber).setCellValue("Номер инженера");
                sheet.getRow(1).createCell(resultCellNumber).setCellValue("Результат");
                boolean nameExists = true;
                int currentRow = 2;
                List<CamStatus> resultList = new ArrayList<>();
                int currentEngineer = 1;
                while (nameExists && !isComplete() && resultList.size() <= maxCamsPerDay) {
                    HSSFCell cellDateNetStatus = null;
                    HSSFCell cellNetStatus = null;
                    HSSFCell cellEngineerNum = null;
                    HSSFCell cellContractor = null;

                    try {
                        HSSFRow row = sheet.getRow(currentRow);
                        if (row == null || row.getCell(7) == null || row.getCell(7).toString().trim().equals("")) {
                            nameExists = false;
                            break;
                        }

                        cellNetStatus = row.createCell(netStatusCellNumber); // netStatus
                        if (cellNetStatus.getStringCellValue().trim().equals("Да"))
                        {
                            continue;
                        }

                        cellContractor = row.getCell(36); // contractor
                        if (!contractor.equals("") && !cellContractor.getStringCellValue().equals(contractor)) {
                            continue;
                        }
                        HSSFCell cellName = row.getCell(2); // name
                        HSSFCell cellIpAddress = row.getCell(8); // ipAddress
                        HSSFCell cellType = row.getCell(7); // type
                        HSSFCell cellCamPort = row.getCell(9); // camPort
                        cellDateNetStatus = row.createCell(dateNetStatusCellNumber); // dateNetStatus
                        cellEngineerNum = row.createCell(engineerNumCellNumber); // engineerNum

                        Camera camera = new Camera(cellName.getStringCellValue().trim(),
                                cellIpAddress.getStringCellValue().trim(),
                                cellType.getStringCellValue().trim(),
                                cellCamPort.getStringCellValue().trim()
                        );


                        resultList.add(new CamStatus(serviceCamsTest.submit(new TaskTestCamera(camera, screensPath, contractor, currentEngineer)),
                                cellDateNetStatus, cellNetStatus, cellEngineerNum, currentEngineer));

                        if ((resultList.size() + 1) % camsPerEngineer == 0 && engineersCountPerDay <= (currentEngineer + 1)) {
                            currentEngineer++;
                        }
                    } catch (Exception e) {
                        LOG.info(e.getMessage());
                    } finally {
                        currentRow++;
                    }
                }

                while (!resultList.isEmpty() && !isComplete()) {
                    Iterator<CamStatus> iterator = resultList.iterator();
                    while (iterator.hasNext() && !isComplete()) {
                        CamStatus camStatus = iterator.next();
                        try {
                            if (camStatus.getTask().isDone()) {
                                if (camStatus.getTask().get()) {
                                    camStatus.getCellNetStatus().setCellValue("Да");
                                    camStatus.getCellDateNetStatus().setCellValue(LocalDate.now().format(DateTimeUtil.DATE_FORMATTER));
                                    camStatus.getCellEngineerNum().setCellValue(camStatus.getEngineerNum());
                                    camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.GREEN.index);
                                } else {
                                    camStatus.getCellNetStatus().setCellValue("Нет потока");
                                    camStatus.getCellDateNetStatus().setCellValue(LocalDate.now().format(DateTimeUtil.DATE_FORMATTER));
                                    camStatus.getCellEngineerNum().setCellValue(camStatus.getEngineerNum());
                                    camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.ORANGE.index);
                                }
                            }
                        } catch (Exception e) {
                            LOG.info(e.getMessage());
                            camStatus.getCellNetStatus().setCellValue("Нет");
                            camStatus.getCellDateNetStatus().setCellValue(LocalDate.now().format(DateTimeUtil.DATE_FORMATTER));
                            camStatus.getCellEngineerNum().setCellValue(camStatus.getEngineerNum());
                            camStatus.getCellNetStatus().getCellStyle().setFillForegroundColor(HSSFColor.RED.index);
                        } finally {
                            if (camStatus.getTask().isDone()) {
                                iterator.remove();
                                camsTestedCount++;
                                camsTestedTodayCount++;
                            }
                            if (isMaxTestedPerDayLock() || isWorkTimeLock()) {
                                saveExcel(myExcelBook, sourcePath);
                                serviceCamsTest.shutdown();
                                serviceMediaPlayer.shutdown();
                                break;
                            }
                        }
                    }
                    if (isMaxTestedPerDayLock() || isWorkTimeLock()) {
                        break;
                    }
                }


                saveExcel(myExcelBook, sourcePath);
                myExcelBook.close();
            } catch (Exception e) {
                LOG.info(e.getMessage());
            }
        }
    }

    public boolean isWorkTimeLock()
    {
        return LocalTime.now().isBefore(startTime) || LocalTime.now().isAfter(endTime);
    }

    public boolean isMaxTestedPerDayLock()
    {
        return camsTestedTodayCount >= maxCamsPerDay && LocalDateTime.now().isBefore(LocalDateTime.of(LocalDate.now().plusDays(1), startTime));
    }

    private void saveExcel(HSSFWorkbook excelBook, String sourcePath)
    {
        boolean writed = false;
        while (!writed) {
            try (FileOutputStream outputStream = new FileOutputStream(sourcePath)) {
                excelBook.write(outputStream);
                writed = true;
            } catch (IOException e) {
                LOG.info(e.getMessage());
            }
        }
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
