package com.grafkom.viewing3d;

import com.grafkom.viewing3d.ProjectionTaxonomyManager.ProjectionType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * ControlPanel = jendela kontrol (Swing) di samping jendela 3D.
 * Berisi menu proyeksi, pengaturan stereo, status, dan petunjuk kontrol.
 * Perintah dikirim ke ThreeDViewing lewat antrean (aman antar thread);
 * tampilan panel disegarkan oleh Timer yang membaca status terbaru.
 */
public class ControlPanel {

    private static final String HELP_TEXT =
            "KAMERA\n"
          + "Mouse kiri + geser : putar kamera\n"
          + "Scroll mouse       : zoom\n"
          + "Panah              : putar kamera\n"
          + "W / S              : zoom (FPS: maju/mundur)\n"
          + "A D Q E            : geser (hanya mode FPS)\n"
          + "TAB                : ganti mode ORBITAL / FPS\n"
          + "R                  : reset kamera\n\n"
          + "PROYEKSI\n"
          + "1 - 6              : pilih jenis proyeksi\n"
          + "Z / X              : FOV kecil / besar (perspektif)\n"
          + "C / V              : Near Plane kecil / besar\n"
          + "B / N              : Far Plane kecil / besar\n"
          + "P                  : reset FOV, Near, Far\n\n"
          + "STEREO\n"
          + "G                  : Anaglyph Red-Cyan on/off\n"
          + "I / O              : IPD kecil / besar\n"
          + "(kacamata: merah di mata kiri, cyan di kanan)\n\n"
          + "LAIN\n"
          + "SPACE              : putar kubus otomatis\n"
          + "ESC                : keluar";

    private final ThreeDViewing app;
    private final JFrame frame;
    private final Map<ProjectionType, JRadioButton> radios = new EnumMap<>(ProjectionType.class);
    private final JCheckBox stereoBox = new JCheckBox("Stereo Anaglyph Red-Cyan (G)");
    private final JCheckBox autoRotateBox = new JCheckBox("Putar kubus otomatis (SPACE)");
    private final JSlider ipdSlider = new JSlider(0, 100, 20);
    private final JLabel ipdLabel = new JLabel("IPD: 0.20");
    private final JTextArea statusArea = new JTextArea(8, 30);
    private final Timer timer;
    private boolean syncing = false;

    public ControlPanel(ThreeDViewing app) {
        this.app = app;
        frame = new JFrame("Panel Kontrol - ThreeDViewing");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        root.add(buildProjectionPanel());
        root.add(Box.createVerticalStrut(6));
        root.add(buildStereoPanel());
        root.add(Box.createVerticalStrut(6));
        root.add(buildButtonPanel());
        root.add(Box.createVerticalStrut(6));
        root.add(buildStatusPanel());
        root.add(Box.createVerticalStrut(6));
        root.add(buildHelpPanel());

        frame.setContentPane(root);
        frame.pack();
        frame.setLocation(905, 10);
        frame.setVisible(true);

        timer = new Timer(150, e -> refresh());
        timer.start();
    }

    private JPanel buildProjectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Proyeksi (tombol 1-6)"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup group = new ButtonGroup();
        for (ProjectionType type : ProjectionType.values()) {
            String kind = type.isPerspective() ? "   [perspektif]" : "   [paralel]";
            JRadioButton radio = new JRadioButton(type.getLabel() + kind);
            radio.addActionListener(e -> app.requestProjection(type));
            group.add(radio);
            radios.put(type, radio);
            panel.add(radio);
        }
        return panel;
    }

    private JPanel buildStereoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Stereo dan tampilan"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        stereoBox.addActionListener(e -> app.requestStereo(stereoBox.isSelected()));
        autoRotateBox.addActionListener(e -> app.requestAutoRotate(autoRotateBox.isSelected()));

        ipdSlider.addChangeListener(e -> {
            ipdLabel.setText(String.format(Locale.US, "IPD: %.2f", ipdSlider.getValue() / 100.0f));
            if (!syncing) {
                app.requestIpd(ipdSlider.getValue() / 100.0f);
            }
        });

        JPanel ipdRow = new JPanel(new BorderLayout(8, 0));
        ipdRow.add(ipdLabel, BorderLayout.WEST);
        ipdRow.add(ipdSlider, BorderLayout.CENTER);
        ipdRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(stereoBox);
        panel.add(autoRotateBox);
        panel.add(ipdRow);
        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 6, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton modeButton = new JButton("Mode (TAB)");
        modeButton.addActionListener(e -> app.requestToggleCameraMode());
        JButton resetCameraButton = new JButton("Reset Kamera");
        resetCameraButton.addActionListener(e -> app.requestResetCamera());
        JButton resetProjectionButton = new JButton("Reset FOV");
        resetProjectionButton.addActionListener(e -> app.requestResetProjection());

        panel.add(modeButton);
        panel.add(resetCameraButton);
        panel.add(resetProjectionButton);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return panel;
    }

    private JPanel buildStatusPanel() {
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHelpPanel() {
        JTextArea helpArea = new JTextArea(HELP_TEXT);
        helpArea.setEditable(false);
        helpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        helpArea.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(helpArea);
        scroll.setPreferredSize(new Dimension(370, 130));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Petunjuk kontrol"));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Menyelaraskan panel dengan keadaan terbaru aplikasi (tombol keyboard ikut tercermin). */
    private void refresh() {
        syncing = true;
        try {
            JRadioButton radio = radios.get(app.getShownType());
            if (radio != null && !radio.isSelected()) {
                radio.setSelected(true);
            }
            stereoBox.setSelected(app.isShownStereo());
            autoRotateBox.setSelected(app.isShownAutoRotate());

            int ipdValue = Math.round(app.getShownIpd() * 100.0f);
            if (ipdSlider.getValue() != ipdValue && !ipdSlider.getValueIsAdjusting()) {
                ipdSlider.setValue(ipdValue);
            }

            String text = app.getStatusText();
            if (!text.equals(statusArea.getText())) {
                statusArea.setText(text);
            }
        } finally {
            syncing = false;
        }
    }
}