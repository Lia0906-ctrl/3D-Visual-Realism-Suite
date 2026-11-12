package com.grafkom.viewing3d;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Locale;

/**
 * InfoPanel = jendela kecil di samping jendela teapot yang menampilkan:
 *  - mode rendering yang sedang aktif (berubah langsung saat tombol ditekan), dan
 *  - daftar tombol kontrol.
 * Panel ini tidak pernah mengambil fokus keyboard, jadi tombol tetap diterima jendela teapot.
 */
public class InfoPanel extends JFrame {

    private static final String ON = "#1a7f37";
    private static final String OFF = "#c62828";

    private final JLabel label = new JLabel();

    public InfoPanel() {
        super("Panel Informasi - 3D Visual Realism Suite");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   // ditutup bersama jendela teapot
        setFocusableWindowState(false);                          // jangan curi fokus keyboard

        label.setVerticalAlignment(JLabel.TOP);
        label.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        label.setText(buildHtml(new RenderSettings(), 0, 0));
        getContentPane().setBackground(Color.WHITE);
        getContentPane().add(label);

        setPreferredSize(new Dimension(330, 560));
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(Math.max(0, screen.width - getWidth() - 10), 40);   // di sisi kanan layar
        setVisible(true);
    }

    /** Dipanggil dari thread OpenGL; pembaruan tampilan dijalankan di thread Swing. */
    public void update(RenderSettings settings, double fps, int triangles) {
        String html = buildHtml(settings, fps, triangles);
        SwingUtilities.invokeLater(() -> label.setText(html));
    }

    private static String state(boolean on, String onText, String offText) {
        return "<font color='" + (on ? ON : OFF) + "'><b>" + (on ? onText : offText) + "</b></font>";
    }

    private static String row(String name, String value, String key) {
        return "<tr><td>" + name + "</td><td>" + value + "</td><td><i>" + key + "</i></td></tr>";
    }

    private static String buildHtml(RenderSettings s, double fps, int triangles) {
        String texture = !s.isUseTexture()
                ? state(false, "", "OFF (warna polos)")
                : "<font color='" + ON + "'><b>" + (s.isBilinearMipmap() ? "Bilinear + Mipmap" : "Nearest") + "</b></font>";
        String shadow = s.isShadows()
                ? "<font color='" + ON + "'><b>ON</b></font> (bias "
                  + String.format(Locale.US, "%.3f", s.getShadowBias()) + ")"
                : state(false, "", "OFF");

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Segoe UI,Arial; font-size:11px;'>");
        sb.append("<div style='font-size:13px;'><b>MODE RENDERING AKTIF</b></div><br>");
        sb.append("<table cellspacing='2' cellpadding='1'>");
        sb.append("<tr><td><u>Fitur</u></td><td><u>Status</u></td><td><u>Tombol</u></td></tr>");
        sb.append(row("Shading model", "<b>" + (s.isBlinnPhong() ? "Blinn-Phong" : "Phong") + "</b>", "B"));
        sb.append(row("Normal", "<b>" + (s.isFlatShading() ? "Flat (per-face)" : "Smooth (rata-rata)") + "</b>", "F"));
        sb.append(row("Z-Buffer", state(s.isDepthTest(), "ON", "OFF"), "Z"));
        sb.append(row("Back-Face Culling", state(s.isBackFaceCulling(), "ON", "OFF"), "C"));
        sb.append(row("Tekstur", texture, "T"));
        sb.append(row("Filter tekstur", "<b>" + (s.isBilinearMipmap() ? "Bilinear Mipmap" : "Nearest") + "</b>", "M"));
        sb.append(row("Bayangan", shadow, "S"));
        sb.append(row("Shadow bias", "<b>" + String.format(Locale.US, "%.3f", s.getShadowBias()) + "</b>", "[ dan ]"));
        sb.append(row("VSync", state(s.isVsync(), "ON", "OFF"), "V"));
        sb.append(row("Putar otomatis", state(s.isAutoRotate(), "ON", "OFF"), "Spasi"));
        sb.append("</table><br>");
        sb.append("<b>FPS:</b> ").append(String.format(Locale.US, "%.0f", fps));
        sb.append(" &nbsp; <b>Segitiga teapot:</b> ").append(triangles).append("<br><br>");
        sb.append("<b>KONTROL LAIN</b><br>");
        sb.append("Mouse: tahan klik kiri lalu geser = putar kamera<br>");
        sb.append("Scroll mouse = zoom in / out<br>");
        sb.append("R = reset kamera &nbsp;|&nbsp; ESC = keluar<br><br>");
        sb.append("<font color='gray'>Klik jendela teapot dulu agar tombol keyboard diterima.</font>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
