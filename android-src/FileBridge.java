package __PACKAGE__;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Native Bruecke, aus der Web-App erreichbar als window.RcAndroid.
 * Uebernommen aus gym-coach\android-src\FileBridge.java und sehkraft-trainer.
 *
 * saveFile(name, text): System-Dialog "Speichern unter", danach
 *   window.rcFileSaved(true|false).
 * openFile(): System-Dateiauswahl; ist die Datei gelesen, ruft die Bruecke
 *   window.rcFileReady(), die Web-App holt den Inhalt per takePickedFile().
 * shareText(text): Android-Teilen-Menue (z.B. an die Claude-App).
 *
 * Die Quelle liegt in reha-coach\android-src und wird von build.ps1 ins
 * generierte Projekt kopiert; MainActivity leitet onActivityResult an
 * onResult() weiter.
 */
public class FileBridge {
    static final int REQ_SAVE = 4861;
    static final int REQ_OPEN = 4862;
    private static final int MAX_BYTES = 30 * 1024 * 1024;

    private final Activity activity;
    private final WebView webView;
    private final Object lock = new Object();
    private String pendingSave = null;
    private String picked = null;

    FileBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public void saveFile(String name, String text) {
        synchronized (lock) { pendingSave = text; }
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            i.putExtra(Intent.EXTRA_TITLE, name);
            start(i, REQ_SAVE, "rcFileSaved", "false");
        });
    }

    @JavascriptInterface
    public void openFile() {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            // JSON hat auf Android keinen verlaesslichen MIME-Typ, deshalb
            // alles zulassen und in JS pruefen.
            i.setType("*/*");
            start(i, REQ_OPEN, null, null);
        });
    }

    /**
     * Legt einen Text in die Zwischenablage (Standardweg fuer den Export in
     * den Health-OS-Chat: dort einfuegen). Nativ statt document.execCommand,
     * weil das im WebView nicht verlaesslich ist.
     */
    @JavascriptInterface
    public void copyText(String text) {
        activity.runOnUiThread(() -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                Toast.makeText(activity, "Kopieren fehlgeschlagen", Toast.LENGTH_SHORT).show();
                return;
            }
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Reha-Coach", text));
        });
    }

    @JavascriptInterface
    public void shareText(String text) {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            try {
                activity.startActivity(Intent.createChooser(i, "Einheit senden an"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, "Keine App zum Teilen gefunden", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public String takePickedFile() {
        synchronized (lock) {
            String s = picked;
            picked = null;
            return s == null ? "" : s;
        }
    }

    @SuppressWarnings("deprecation")
    private void start(Intent i, int req, String failFn, String failArg) {
        try {
            activity.startActivityForResult(i, req);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "Keine Dateiauswahl verfuegbar", Toast.LENGTH_SHORT).show();
            if (failFn != null) js(failFn, failArg);
        }
    }

    /** Aus MainActivity.onActivityResult. true, wenn das Ergebnis uns gehoert. */
    boolean onResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_SAVE && requestCode != REQ_OPEN) return false;
        Uri uri = (resultCode == Activity.RESULT_OK && data != null) ? data.getData() : null;

        if (requestCode == REQ_SAVE) {
            String text;
            synchronized (lock) { text = pendingSave; pendingSave = null; }
            if (uri == null || text == null) { js("rcFileSaved", "false"); return true; }
            try (OutputStream os = activity.getContentResolver().openOutputStream(uri, "wt")) {
                if (os == null) throw new java.io.IOException("kein Stream");
                os.write(text.getBytes(StandardCharsets.UTF_8));
                js("rcFileSaved", "true");
            } catch (Exception e) {
                Toast.makeText(activity, "Speichern fehlgeschlagen", Toast.LENGTH_LONG).show();
                js("rcFileSaved", "false");
            }
            return true;
        }

        if (uri == null) return true;   // Auswahl abgebrochen
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new java.io.IOException("kein Stream");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) throw new java.io.IOException("Datei zu gross");
            }
            synchronized (lock) { picked = new String(out.toByteArray(), StandardCharsets.UTF_8); }
            js("rcFileReady", "");
        } catch (Exception e) {
            Toast.makeText(activity, "Datei konnte nicht gelesen werden", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void js(String fn, String arg) {
        webView.post(() -> webView.evaluateJavascript(
            "window." + fn + " && window." + fn + "(" + arg + ")", null));
    }
}
