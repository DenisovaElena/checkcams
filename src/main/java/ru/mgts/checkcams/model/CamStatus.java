package ru.mgts.checkcams.model;

import org.apache.poi.hssf.usermodel.HSSFCell;

import java.util.concurrent.Future;

/**
 * Created by Administrator on 16.03.2017.
 */
public class CamStatus {
    private Future<Boolean> task;
    private HSSFCell cellDateNetStatus;
    private HSSFCell cellNetStatus;
    private HSSFCell cellEngineerNum;
    private int engineerNum;

    public CamStatus(Future<Boolean> task, HSSFCell cellDateNetStatus, HSSFCell cellNetStatus, HSSFCell cellEngineerNum, int engineerNum) {
        this.task = task;
        this.cellDateNetStatus = cellDateNetStatus;
        this.cellNetStatus = cellNetStatus;
        this.cellEngineerNum = cellEngineerNum;
        this.engineerNum = engineerNum;
    }

    public Future<Boolean> getTask() {
        return task;
    }

    public HSSFCell getCellDateNetStatus() {
        return cellDateNetStatus;
    }

    public HSSFCell getCellNetStatus() {
        return cellNetStatus;
    }

    public HSSFCell getCellEngineerNum() {
        return cellEngineerNum;
    }

    public int getEngineerNum() {
        return engineerNum;
    }
}
