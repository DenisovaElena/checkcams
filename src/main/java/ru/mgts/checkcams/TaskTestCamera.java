package ru.mgts.checkcams;

import com.sun.jna.NativeLibrary;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mgts.checkcams.model.CamStatus;
import ru.mgts.checkcams.model.Camera;
import ru.mgts.checkcams.model.RTSPdata;
import ru.mgts.checkcams.util.DateTimeUtil;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static ru.mgts.checkcams.CameraChecker.rtspDataList;
import static ru.mgts.checkcams.CameraChecker.serviceMediaPlayer;

/**
 * Created by Administrator on 16.03.2017.
 */
public class TaskTestCamera implements Callable<Boolean> {

    protected static final Logger LOG = LoggerFactory.getLogger(CControl.class);
    private Camera camera;
    private String screensPath;
    private LocalTime startTime;
    private LocalTime endTime;

    public TaskTestCamera(Camera camera, String screensPath, LocalTime startTime, LocalTime endTime) {
        this.camera = camera;
        this.screensPath = screensPath;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public Boolean call() throws Exception {
        Thread.currentThread().setName("Thread test for camera: " + camera.getName() + " with ip " + camera.getIpAddress());
        while (LocalTime.now().isBefore(startTime) || LocalTime.now().isAfter(endTime) || CameraChecker.isPaused())
        {
            Thread.sleep(1000);
        }

        MediaPlayer mediaPlayer = initMediaPlayer();

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
        if (!rtspData.getCamClass().equals("PVN") && !rtspData.getCamClass().equals("OVN")) {
            rtspAddress = String.format("rtsp://%s:%s@%s:%s%s",
                    rtspData.getLogin(), rtspData.getPass(),
                    camera.getIpAddress(), rtspData.getPort(),
                    rtspData.getChannel());

            // маска имени файла, начиная с папки. Разеделние на папки через /
            screenNameMask =
                    screensPath +
                            "/" + getNowDate() +
                            "/" +                 //сюда вписать папку Номер инженера
                            "/" + rtspData.getFolderName() +
                            "/" + camera.getName() + "_IP" + camera.getIpAddress() + ".png";
        } else {
            String channel = camera.getCamPort().equals("1") && rtspData.getCamClass().equals("PVN") ?
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
                            "/" +                    //сюда вписать паку Номер инженера
                            "/" + rtspData.getFolderName() +
                            "/" + camera.getName() +"_IP" + camera.getIpAddress() +
                            "/" + camera.getName() + ".png";
        }

        return saveScreen(rtspAddress, screenNameMask, mediaPlayer, 5);
    }

    private String getNowDate()
    {
        return LocalDate.now().format(DateTimeUtil.DATE_FORMATTER);
    }

    private MediaPlayer initMediaPlayer()
    {
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\vlc");
        MediaPlayerFactory factory = new MediaPlayerFactory();
        return factory.newEmbeddedMediaPlayer();
    }

    private boolean saveScreen(final String rtspAddress, final String savePath, final MediaPlayer mediaPlayer, int repeatsCount)
    {
        try {
            LOG.debug("ACHTUNG! Starting vlc for stream " + rtspAddress);

            Runnable taskPlay = () -> { Thread.currentThread().setName("Thread vlc for: " + rtspAddress); mediaPlayer.playMedia(rtspAddress); };
            Future task =serviceMediaPlayer.submit(taskPlay);
            Thread.sleep(50000);
            File file = new File(savePath);
            mediaPlayer.saveSnapshot(file);
            mediaPlayer.stop();
            while (!task.isDone())
            {
                Thread.sleep(1);
            }
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
    private void disableControls()
    {

    }
}
