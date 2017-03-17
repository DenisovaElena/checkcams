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
 * Created by MakhrovSS on 15.03.2017.
 */
public class MainForm extends JFrame{

    private JPanel rootPanel;
    private JButton buttonStartChecker;
    private JLabel labelStartTime;
    private JLabel labelEndTime;
    private JFormattedTextField textStartTime;
    private JFormattedTextField textEndTime;
    private JLabel labelSourcePath;
    private JLabel labelDestinationPath;
    private JButton buttonBrowseSourcePath;
    private JButton buttonBrowseDestinationPath;
    private JTextField textSourcePath;
    private JTextField textDestinationPath;
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

    public MainForm()
    {
        setContentPane(rootPanel);

        //textStartTime = new JFormattedTextField(TIME_FORMATTER_FORMATTER);

        textStartTime.setText("08:00");
        textEndTime.setText("17:00");
        spinnerCamsCount.setValue(1000);


        pack();
        setTitle("Опрос камер");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 400);
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
                    String destinationPath = textDestinationPath.getText().trim();
                    String screensPath = textScreensPath.getText().trim();
                    LocalTime startTime = DateTimeUtil.parseLocalTime(textStartTime.getText());
                    LocalTime endTimeTime = DateTimeUtil.parseLocalTime(textEndTime.getText());
                    int maxCamsPerDay = (Integer) spinnerCamsCount.getValue();
                    String contractor = String.valueOf(comboBoxContractor.getSelectedItem());
                    if (!(new File(sourcePath).exists()))
                    {
                        showMessageDialog(null, "Не удается найти исходный файл");
                    }
                    if (!(new File(destinationPath).exists()))
                    {
                        showMessageDialog(null, "Не указан результирующий файл");
                    }
                    if (!(new File(screensPath).exists()))
                    {
                        showMessageDialog(null, "Не указана папка для снимков экрана");
                    }



                    buttonStartChecker.setEnabled(false);
                    cameraChecker.reset();
                    Thread checkerThread = new Thread() {
                        @Override
                        public void run() {
                            textAreaLog.setText("Опрос начат\n");
                            cameraChecker.startCameraIterator(
                                    sourcePath,
                                    destinationPath,
                                    screensPath,
                                    startTime,
                                    endTimeTime,
                                    maxCamsPerDay,
                                    contractor
                            );
                        }
                    };

                    Thread checkerListener = new Thread() {
                        @Override
                        public void run() {
                            cameraChecker.setComplete(false);
                            while (!cameraChecker.isComplete()) {
                                labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                                if (!isWorkTime()) {
                                    textAreaLog.append("Опрос приостановлен до начала рабочего времени\n");
                                    while (!isWorkTime() && !cameraChecker.isComplete()) {
                                    }
                                    textAreaLog.append("Опрос возобновлен\n");
                                }
                            }

                            try {
                                checkerThread.join();
                                textAreaLog.append("Опрос завершен\n");
                                if (cameraChecker.getCamsTestedCount() == 0) {
                                    showMessageDialog(null, "Не найдено ни одной камеры");
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            buttonStartChecker.setEnabled(true);
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
        buttonBrowseDestinationPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setPreferredSize(new Dimension(800, 600));
                fileChooser.setFileFilter(new FileNameExtensionFilter("Excel XLS files", "xls"));
                fileChooser.setCurrentDirectory(new java.io.File("."));
                fileChooser.showSaveDialog(rootPanel);
                if (fileChooser.getSelectedFile() != null)
                    textDestinationPath.setText(fileChooser.getSelectedFile().getAbsolutePath());
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
                serviceMediaPlayer.shutdown();
                serviceCamsTest.shutdown();
            }
        });
    }

    private boolean isWorkTime()
    {
        boolean result = false;
        try {
            if (!LocalTime.now().isBefore(DateTimeUtil.parseLocalTime(textStartTime.getText()))
                    && !LocalTime.now().isAfter(DateTimeUtil.parseLocalTime(textEndTime.getText()))) {
                result = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return result;
    }

    private void createUIComponents() {
        comboBoxContractor = new JComboBox(Configurator.loadConfigsContractor().toArray());
        comboBoxContractor.addItem("");
    }
}
