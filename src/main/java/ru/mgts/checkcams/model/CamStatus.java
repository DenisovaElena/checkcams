package ru.mgts.checkcams.model;

import org.apache.poi.hssf.usermodel.HSSFCell;

import java.util.concurrent.Future;

/**
 * Created by Administrator on 16.03.2017.
 */
public class CamStatus {
    private Future<Boolean> task;
    private HSSFCell cellNetStatus;

    public CamStatus(Future<Boolean> task, HSSFCell cellNetStatus) {
        this.task = task;
        this.cellNetStatus = cellNetStatus;
    }

    public Future<Boolean> getTask() {
        return task;
    }

    public HSSFCell getCellNetStatus() {
        return cellNetStatus;
    }
}
