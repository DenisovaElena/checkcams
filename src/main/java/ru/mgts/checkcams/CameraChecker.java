package ru.mgts.checkcams;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
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
 * Created by Dummy on 15.03.2017.
 */
public class CameraChecker {

    protected static final Logger LOG = LoggerFactory.getLogger(CControl.class);

    private volatile boolean complete;
    private static volatile boolean killThreads;
    private int camsTestedCount;
    private int camsTestedTodayCount;

    private String sourcePath;
    private String screensPath;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxCamsPerDay;
    private String region;
    private int engineersCountPerDay;
    private LocalDateTime currentTestDateTime;

    protected static ExecutorService serviceCamsTest;
    public static Map<String, RTSPdata> rtspDataList = Configurator.loadConfigsRTSP();

    public CameraChecker() {
    }

    public void init(String sourcePath, String screensPath, LocalTime startTime, LocalTime endTime, int maxCamsPerDay, String region, int engineersCountPerDay)
    {
        this.sourcePath = sourcePath;
        this.screensPath = screensPath;
        this.region = region;
        this.engineersCountPerDay = engineersCountPerDay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCamsPerDay = maxCamsPerDay;
        this.currentTestDateTime = null;
    }

    public void reset()
    {
        ThreadFactory threadFactoryCamsTest = new ThreadFactoryBuilder()
                .setNameFormat("camsTest-%d")
                .build();
        serviceCamsTest = Executors.newFixedThreadPool(5, threadFactoryCamsTest);
        CameraChecker.killThreads = false;

    }

    public String getStringCellVal(HSSFCell cell)
    {
        String cellValue = null;

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                cellValue = cell.getStringCellValue();
                break;

            case Cell.CELL_TYPE_FORMULA:
                cellValue = cell.getCellFormula();
                break;

            case Cell.CELL_TYPE_NUMERIC:
                    cellValue = Integer.toString((int)cell.getNumericCellValue());
                break;

            case Cell.CELL_TYPE_BLANK:
                cellValue = "";
                break;

            case Cell.CELL_TYPE_BOOLEAN:
                cellValue = Boolean.toString(cell.getBooleanCellValue());
                break;
        }
        return cellValue.trim();
    }

    public void recalcNums(String sourcePath, int maxCamsPerDay, String region, int engineersCountPerDay)
    {
        this.sourcePath = sourcePath;
        this.maxCamsPerDay = maxCamsPerDay;
        this.region = region;
        this.engineersCountPerDay = engineersCountPerDay;


        try (InputStream inputStream = new FileInputStream(sourcePath)) {

            int camsTestedCount = 0;
            int camsPerEngineer = maxCamsPerDay / engineersCountPerDay;
            int remainder = maxCamsPerDay % engineersCountPerDay;
            int remainderCounter = remainder == 0 ? remainder : remainder - 1;
            int remainderCompensator = 0;
            int correctionUnit = remainder == 0 ? 0 : 1;

            HSSFWorkbook myExcelBook = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = myExcelBook.getSheetAt(0);
            int netStatusCellNumber = 52;
            int engineerNumCellNumber = 53;

            sheet.getRow(1).createCell(netStatusCellNumber).setCellValue("Скрин-да/нет");
            sheet.getRow(1).createCell(engineerNumCellNumber).setCellValue("Номер инженера");
            boolean screenStateExists = true;
            int currentRow = 2;
            int currentEngineer = 1;
            while (screenStateExists && camsTestedCount < maxCamsPerDay) {
                HSSFCell cellEngineerNum = null;
                HSSFCell cellRegion = null;

                try {
                    HSSFRow row = sheet.getRow(currentRow);
                    if (row == null || row.getCell(netStatusCellNumber) == null || row.getCell(netStatusCellNumber).toString().trim().equals(""))
                    {
                        screenStateExists = false;
                        break;
                    }

                    cellRegion = row.getCell(6); // region
                    if (!region.equals("") && !getStringCellVal(cellRegion).startsWith(region)) {
                        continue;
                    }
                    camsTestedCount++;

                    cellEngineerNum = row.createCell(engineerNumCellNumber); // engineerNum
                    cellEngineerNum.setCellValue(currentEngineer);

                    if ((camsTestedCount - remainderCompensator) % (camsPerEngineer + correctionUnit) == 0 && currentEngineer < engineersCountPerDay) {
                        currentEngineer++;
                        if (remainderCounter != 0)
                        {
                            remainderCounter--;
                            correctionUnit = 1;
                        }
                        else
                        {
                            correctionUnit = 0;
                            if (remainder != 0) {
                                remainderCompensator = remainder;
                            }
                        }
                    }


                } catch (Exception e) {
                    LOG.info(e.getMessage());
                } finally {
                    currentRow++;
                }
            }

            saveExcel(myExcelBook, sourcePath);
            myExcelBook.close();
        } catch (Exception e) {
            LOG.info(e.getMessage());
        }
    }

    public void startCameraIterator() {
        while (!isComplete()) {
            try (InputStream inputStream = new FileInputStream(sourcePath)) {

                //saveScreen("rtsp://admin:admin@10.209.246.42:554/channel1", "C:\\screens\\test.png", mediaPlayer);
                //saveScreen("file:///C:\\Szamar Madar.avi", "C:\\screens\\test.png", mediaPlayer);
                //saveScreen("rtsp://localhost:5544/pusya", "C:\\screens\\test.png", mediaPlayer, 3);

                reset();
                currentTestDateTime = null;

                camsTestedCount = 0;
                camsTestedTodayCount = 0;
                int camsPerEngineer = maxCamsPerDay / engineersCountPerDay;
                int remainder = maxCamsPerDay % engineersCountPerDay;
                int remainderCounter = remainder == 0 ? remainder : remainder - 1;
                int remainderCompensator = 0;
                int correctionUnit = remainder == 0 ? 0 : 1;

                HSSFWorkbook myExcelBook = new HSSFWorkbook(inputStream);
                HSSFSheet sheet = myExcelBook.getSheetAt(0);
                int dateNetStatusCellNumber = 51;
                int netStatusCellNumber = 52;
                int engineerNumCellNumber = 53;
                int problemCellNumber= 54;
                int resultCellNumber = 55;

                sheet.getRow(1).createCell(dateNetStatusCellNumber).setCellValue("Дата опроса");
                sheet.getRow(1).createCell(netStatusCellNumber).setCellValue("Скрин-да/нет");
                sheet.getRow(1).createCell(engineerNumCellNumber).setCellValue("Номер инженера");
                sheet.getRow(1).createCell(resultCellNumber).setCellValue("Заявка");
                sheet.getRow(1).createCell(problemCellNumber).setCellValue("Загрязнение");

                boolean nameExists = true;
                int currentRow = 2;
                List<CamStatus> resultList = new ArrayList<>();
                int currentEngineer = 1;
                while (nameExists && !isComplete() && resultList.size() < maxCamsPerDay) {
                    HSSFCell cellDateNetStatus = null;
                    HSSFCell cellNetStatus = null;
                    HSSFCell cellEngineerNum = null;
                    HSSFCell cellRegion = null;

                    try {
                        HSSFRow row = sheet.getRow(currentRow);
                        if (row == null || row.getCell(7) == null || row.getCell(7).toString().trim().equals("")) {
                            nameExists = false;
                            break;
                        }

                         // netStatus
                        cellNetStatus = row.getCell(netStatusCellNumber);
                        if (
                                cellNetStatus != null && cellNetStatus.getCellType() != Cell.CELL_TYPE_BLANK //&&
                                //        (cellNetStatus.getStringCellValue().trim().equals("Да") ||
                                //cellNetStatus.getStringCellValue().trim().equals("Нет"))
                                )
                        {
                            continue;
                        }
                        else {
                            cellNetStatus = row.createCell(netStatusCellNumber);
                        }

                        cellRegion = row.getCell(6); // region
                        if (!region.equals("") && !getStringCellVal(cellRegion).startsWith(region)) {
                            continue;
                        }
                        HSSFCell cellName = row.getCell(2); // name
                        HSSFCell cellIpAddress = row.getCell(8); // ipAddress
                        HSSFCell cellType = row.getCell(7); // type
                        HSSFCell cellCamPort = row.getCell(9); // camPort
                        cellDateNetStatus = row.createCell(dateNetStatusCellNumber); // dateNetStatus
                        cellEngineerNum = row.createCell(engineerNumCellNumber); // engineerNum

                        Camera camera = new Camera(getStringCellVal(cellName),
                                getStringCellVal(cellIpAddress),
                                getStringCellVal(cellType),
                                getStringCellVal(cellCamPort)
                        );


                        resultList.add(new CamStatus(serviceCamsTest.submit(new TaskTestCamera(camera, screensPath, region, currentEngineer)),
                                cellDateNetStatus, cellNetStatus, cellEngineerNum, currentEngineer));

                        if ((resultList.size() - remainderCompensator) % (camsPerEngineer + correctionUnit) == 0 && currentEngineer < engineersCountPerDay) {
                            currentEngineer++;
                            if (remainderCounter != 0)
                            {
                                remainderCounter--;
                                correctionUnit = 1;
                            }
                            else
                            {
                                correctionUnit = 0;
                                if (remainder != 0) {
                                    remainderCompensator = remainder;
                                }
                            }
                        }


                    } catch (Exception e) {
                        LOG.info(e.getMessage());
                    } finally {
                        currentRow++;
                    }
                }

                while (!resultList.isEmpty() && !isComplete() && !isMaxTestedPerDayLock() && !isWorkTimeLock() && !isPassedListAtThisDayLock()) {
                    Iterator<CamStatus> iterator = resultList.iterator();
                    while (iterator.hasNext() && !isComplete() && !isMaxTestedPerDayLock() && !isWorkTimeLock() && !isPassedListAtThisDayLock()) {
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
                        }
                    }
                }

                saveExcel(myExcelBook, sourcePath);
                myExcelBook.close();

                setKillThreads(true);
                serviceCamsTest.shutdown();
                final boolean doneServiceCamsTest = serviceCamsTest.awaitTermination(5, TimeUnit.SECONDS);
                LOG.debug("ACHTUNG! Is all threads for serviceCamsTest completed? {}", doneServiceCamsTest);
            } catch (Exception e) {
                LOG.info(e.getMessage());
            }

            try {
                currentTestDateTime = LocalDateTime.now();
                while ((isMaxTestedPerDayLock() || isWorkTimeLock() || isPassedListAtThisDayLock()) && !isComplete()) {
                    Thread.sleep(1000);
                }
            }
            catch (InterruptedException e)
            {
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
        return  currentTestDateTime != null && camsTestedTodayCount >= maxCamsPerDay && LocalDateTime.now().isBefore(LocalDateTime.of(currentTestDateTime.toLocalDate().plusDays(1), startTime));
    }

    public boolean isPassedListAtThisDayLock()
    {
        return camsTestedTodayCount > 0 && currentTestDateTime != null && LocalDateTime.now().isBefore(LocalDateTime.of(currentTestDateTime.toLocalDate().plusDays(1), startTime));
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

    public static boolean isKillThreads() {
        return killThreads;
    }

    public static void setKillThreads(boolean killThreads) {
        CameraChecker.killThreads = killThreads;
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
