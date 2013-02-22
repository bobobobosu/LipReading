package edu.lipreading.gui;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import edu.lipreading.Constants;
import edu.lipreading.Sample;
import edu.lipreading.TrainingSet;
import edu.lipreading.Utils;
import edu.lipreading.classification.Classifier;
import edu.lipreading.classification.TimeWarperClassifier;
import edu.lipreading.normalization.CenterNormalizer;
import edu.lipreading.normalization.Normalizer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * @author Dor Leitman
 *
 */

public class FileLipReaderPanel extends VideoCapturePanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JLabel lblOutput;
    private JButton btnRecord;
    private Classifier classifier;
    private Normalizer normalizer;
    private JTextPane txtFilePath;
    private Sample recordedSample;
    final JFileChooser fileChooser = new JFileChooser();

    /**
     * Create the panel.
     */
    public FileLipReaderPanel() {
        super();
        canvas.setBackground(UIManager.getColor("InternalFrame.inactiveTitleGradient"));
        setBackground(Color.WHITE);
        setLayout(null);

        fileChooser.setFileFilter(new VideoFileFilter());

        lblOutput = new JLabel("");
        lblOutput.setHorizontalAlignment(SwingConstants.CENTER);
        lblOutput.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblOutput.setForeground(Color.GRAY);
        lblOutput.setBounds(244, 447, 204, 22);
        this.add(lblOutput);

        canvas.setBounds(142, 10, 420, 308);

        txtFilePath = new JTextPane();
        txtFilePath.setToolTipText("Please insert a URL or choose a file");
        txtFilePath.setBackground(SystemColor.info);
        txtFilePath.setText("https://dl.dropbox.com/u/8720454/set2/no/no-1.MOV"); //TODO - Change default
        txtFilePath.setBounds(204, 337, 320, 20);
        add(txtFilePath);

        List<Sample> trainingSet = TrainingSet.get();

        classifier = new TimeWarperClassifier();
        classifier.train(trainingSet);
        normalizer = new CenterNormalizer();

        final String recordButtonText = "Read Lips From File";
        btnRecord = new JButton(recordButtonText);
        btnRecord.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                btnRecord.setEnabled(false);
                btnRecord.setText("Downloading File...");
                lblOutput.setText("");

                Thread videoGrabberThread = new Thread(new Runnable()
                {
                    public void run()
                    {
                        setVideoInput(txtFilePath.getText());
                        try {
                            initGrabber();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        recordedSample = new Sample(txtFilePath.getText());

                        try {
                            startVideo();
                        } catch (Exception e) {
                            btnRecord.setEnabled(true);
                            btnRecord.setText(recordButtonText);
                        }
                    }
                });
                videoGrabberThread.start();
            }
        });
        btnRecord.setBackground(Color.WHITE);
        btnRecord.setBounds(275, 387, 143, 23);
        this.add(btnRecord);

        JLabel lblNewLabel = new JLabel("File Path:");
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblNewLabel.setBounds(142, 339, 53, 14);
        add(lblNewLabel);

        JButton btnChooseFile = new JButton(new ImageIcon(getClass().getResource(Constants.FILE_CHOOSER_IMAGE_FILE_PATH)));
        btnChooseFile.setBorderPainted(false);
        btnChooseFile.setBackground(Color.WHITE);
        btnChooseFile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                int returnVal = fileChooser.showOpenDialog(FileLipReaderPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION){
                    txtFilePath.setText(fileChooser.getSelectedFile().getPath());
                    progressBar.setVisible(false);
                }
            }
        });
        btnChooseFile.setBounds(530, 333, 32, 32);
        add(btnChooseFile);

        progressBar.setBounds(0, 494, 715, 18);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        add(progressBar);
    }

    @Override
    protected void getVideoFromSource() throws Exception {
        try {
            IplImage grabbed;

            while((grabbed = grabber.grab()) != null && !threadStop.get()){
                image = grabbed.getBufferedImage();
                canvas.setImage(image);
                canvas.paint(null);
                recordedSample.getMatrix().add(stickersExtractor.getPoints(grabbed));
            }
            stopVideo();
            canvas.setImage(null);
            canvas.paint(null);


            Thread classifierThread = new Thread(new Runnable() {

                public void run() {
                    recordedSample = normalizer.normalize(recordedSample);
                    final String outputText = classifier.test(recordedSample);
                    lblOutput.setText(outputText);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Utils.textToSpeech(outputText);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                    btnRecord.setText("Read Lips From File");
                    btnRecord.setEnabled(true);

                }
            });
            classifierThread.start();

        } catch (com.googlecode.javacv.FrameGrabber.Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            grabber.stop();
        }
    }

    class VideoFileFilter extends FileFilter {
        public boolean accept(File file) {
            String filename = file.getName().toLowerCase();
            return filename.endsWith(".mov") ||
                    filename.endsWith(".mpeg") ||
                    filename.endsWith(".mpg") ||
                    filename.endsWith(".wmv") ||
                    filename.endsWith(".mp4") ||
                    filename.endsWith(".3gp") ||
                    filename.endsWith(".avi") ||
                    filename.endsWith(".mkv") ||
                    file.isDirectory();
        }
        public String getDescription() {
            return "video files";
        }
    }
}
