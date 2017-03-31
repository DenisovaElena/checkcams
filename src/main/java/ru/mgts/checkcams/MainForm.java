package ru.mgts.checkcams;

import ru.mgts.checkcams.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static javax.swing.JOptionPane.showMessageDialog;
import static ru.mgts.checkcams.CameraChecker.serviceCamsTest;
import static ru.mgts.checkcams.CameraChecker.serviceMediaPlayer;

/**
 * Created by Dummy on 15.03.2017.
 */
public class MainForm extends JFrame{

    private JPanel rootPanel;
    private JButton buttonStartChecker;
    private JLabel labelStartTime;
    private JLabel labelEndTime;
    private JFormattedTextField textStartTime;
    private JFormattedTextField textEndTime;
    private JLabel labelSourcePath;
    private JButton buttonBrowseSourcePath;
    private JTextField textSourcePath;
    private JLabel labelScreensPath;
    private JTextField textScreensPath;
    private JButton buttonBrowseScreensPath;
    private JLabel camsTested;
    private JLabel labelCamsTestedCounter;
    private JTextArea textAreaLog;
    private JButton buttonEnd;
    private JLabel labelCamsCount;
    private JSpinner spinnerCamsCount;
    private JLabel labelContractor;
    private JComboBox comboBoxContractor;
    private JSpinner spinnerEngineersCount;
    private JLabel labelEngineersCount;
    private JButton buttonRecalcNums;

    public MainForm()
    {
        setContentPane(rootPanel);

        //textStartTime = new JFormattedTextField(TIME_FORMATTER_FORMATTER);

        textStartTime.setText("08:00");
        textEndTime.setText("17:00");
        spinnerCamsCount.setValue(1000);
        spinnerEngineersCount.setValue(5);


        pack();
        setTitle("Опрос камер");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 400);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        CameraChecker cameraChecker = new CameraChecker();

        buttonEnd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraChecker.setComplete(true);
            }
        });
        buttonStartChecker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String sourcePath = textSourcePath.getText().trim();
                    String screensPath = textScreensPath.getText().trim();
                    LocalTime startTime = DateTimeUtil.parseLocalTime(textStartTime.getText());
                    LocalTime endTimeTime = DateTimeUtil.parseLocalTime(textEndTime.getText());
                    int maxCamsPerDay = (Integer) spinnerCamsCount.getValue();
                    int engineersCountPerDay = (Integer) spinnerEngineersCount.getValue();
                    String contractor = String.valueOf(comboBoxContractor.getSelectedItem());
                    if (!(new File(sourcePath).exists()))
                    {
                        showMessageDialog(null, "Не удается найти исходный файл");
                    }
                    if (!(new File(screensPath).exists()))
                    {
                        showMessageDialog(null, "Не указана папка для снимков экрана");
                    }
                    if (engineersCountPerDay <= 0)
                    {
                        showMessageDialog(null, "Количество инженеров должно быть больше нуля");
                    }

                    if (engineersCountPerDay > maxCamsPerDay)
                    {
                        showMessageDialog(null, "Количество инженеров не должно превышать количество камер");
                    }

                    buttonStartChecker.setEnabled(false);
                    buttonRecalcNums.setEnabled(false);
                    cameraChecker.init(
                            sourcePath,
                            screensPath,
                            startTime,
                            endTimeTime,
                            maxCamsPerDay,
                            contractor,
                            engineersCountPerDay
                    );
                    Thread checkerThread = new Thread() {
                        @Override
                        public void run() {
                            textAreaLog.setText("Опрос начат\n");
                            cameraChecker.startCameraIterator();
                        }
                    };

                    Thread checkerListener = new Thread() {
                        @Override
                        public void run() {
                            cameraChecker.setComplete(false);
                            while (!cameraChecker.isComplete()) {
                                labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                                if (cameraChecker.isWorkTimeLock()) {
                                    textAreaLog.append("Опрос приостановлен до начала рабочего времени\n");
                                    labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                                    while (cameraChecker.isWorkTimeLock() && !cameraChecker.isComplete()) {
                                    }
                                    textAreaLog.append("Опрос возобновлен\n");
                                }

                                if (cameraChecker.isMaxTestedPerDayLock()) {
                                    textAreaLog.append("Опрос приостановлен поскольку достигнут лимит опроса в день\n");
                                    labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                                    while (cameraChecker.isMaxTestedPerDayLock() && !cameraChecker.isComplete()) {
                                    }
                                    textAreaLog.append("Опрос возобновлен\n");
                                }

                                if (cameraChecker.isPassedListAtThisDayLock()) {
                                    textAreaLog.append("Опрос на сегодня приостановлен, поскольку пройдены все камеры\n");
                                    labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                                    while (cameraChecker.isPassedListAtThisDayLock() && !cameraChecker.isComplete()) {
                                    }
                                    textAreaLog.append("Опрос возобновлен\n");
                                }
                            }

                            try {
                                checkerThread.join();
                                textAreaLog.append("Опрос завершен\n");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            buttonStartChecker.setEnabled(true);
                            buttonRecalcNums.setEnabled(true);
                        }
                    };

                    checkerThread.start();
                    checkerListener.start();
                }
                catch (Exception err)
                {
                    showMessageDialog(null, err.getMessage());
                }
            }
        });
        buttonBrowseSourcePath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setPreferredSize(new Dimension(800, 600));
                fileChooser.setFileFilter(new FileNameExtensionFilter("Excel XLS files", "xls"));
                fileChooser.setCurrentDirectory(new java.io.File("."));
                fileChooser.showOpenDialog(rootPanel);
                if (fileChooser.getSelectedFile() != null)
                    textSourcePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        buttonBrowseScreensPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setPreferredSize(new Dimension(800, 600));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new java.io.File("."));
                fileChooser.showOpenDialog(rootPanel);
                if (fileChooser.getSelectedFile() != null)
                    textScreensPath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {

            }
        });
        buttonRecalcNums.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sourcePath = textSourcePath.getText().trim();
                int maxCamsPerDay = (Integer) spinnerCamsCount.getValue();
                int engineersCountPerDay = (Integer) spinnerEngineersCount.getValue();
                String contractor = String.valueOf(comboBoxContractor.getSelectedItem());
                if (!(new File(sourcePath).exists()))
                {
                    showMessageDialog(null, "Не удается найти исходный файл");
                }
                if (engineersCountPerDay <= 0)
                {
                    showMessageDialog(null, "Количество инженеров должно быть больше нуля");
                }

                if (engineersCountPerDay > maxCamsPerDay)
                {
                    showMessageDialog(null, "Количество инженеров не должно превышать количество камер");
                }

                cameraChecker.recalcNums(sourcePath, maxCamsPerDay, contractor, engineersCountPerDay);
                textAreaLog.append("Инженеры перераспределены\n");
            }
        });
    }

    private void createUIComponents() {
        comboBoxContractor = new JComboBox(Configurator.loadConfigsContractor().toArray());
        comboBoxContractor.addItem("");
    }
}
