package ru.mgts.checkcams;

import ru.mgts.checkcams.util.DateTimeUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;

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

    public MainForm()
    {
        setContentPane(rootPanel);

        //textStartTime = new JFormattedTextField(TIME_FORMATTER_FORMATTER);

        textStartTime.setText("08:00");
        textEndTime.setText("17:00");

        pack();
        setTitle("Опрос камер");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 400);
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
                buttonStartChecker.setEnabled(false);
                Thread checkerThread = new Thread()
                {
                    @Override
                    public void run() {
                        textAreaLog.append("Опрос начат\n");
                        cameraChecker.startCameraIterator(
                                textSourcePath.getText().trim(),
                                textDestinationPath.getText().trim(),
                                textScreensPath.getText().trim(),
                                DateTimeUtil.parseLocalTime(textStartTime.getText()),
                                DateTimeUtil.parseLocalTime(textEndTime.getText())
                        );
                    }
                };

                Thread checkerListener = new Thread()
                {
                    @Override
                    public void run() {
                        while (!cameraChecker.isComplete())
                        {
                            labelCamsTestedCounter.setText(cameraChecker.getCamsTestedCount() + "");
                        }
                        textAreaLog.append("Опрос завершен\n");
                        buttonStartChecker.setEnabled(true);
                    }
                };

                checkerThread.start();
                checkerListener.start();
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
    }

}
