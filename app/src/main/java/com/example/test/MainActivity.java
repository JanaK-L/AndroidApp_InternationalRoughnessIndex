package com.example.test;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.Menu;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.GroundOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Klassenvariablen
    // repräsentiert die Karte
    private MapView map;
    // repräsentiert die aktuelle Position/ den aktuellen Positionsmarker auf der Karte
    private Marker userPosMarker;
    // Location Client, stellt den Standortdienst zur Verfügung
    private FusedLocationProviderClient fusedLocationClient;
    // App Kontext
    private Context ctx;
    // Geocoder, der für die Geocodierung benötigt wird
    private Geocoder geo;
    // repräsentiert die aktuelle Straße
    private String strasse;
    // zum Speichern aller Punkte des aktuellen Segments
    private ArrayList<GeoPoint> aktuSeg;
    // zum Speichern aller gesamtheitlich abgefahrenen Punkte, gegliedert nach Segmenten
    private ArrayList<ArrayList<GeoPoint>> abgefahreneSegmente;
    // zum Speichern aller IRIwerte innerhalb eines Segments
    private ArrayList<Double> iriwerte;
    // Variable, die wahr ist, wenn die Zentrierung an ist
    private boolean zentriert;
    // Variable, die wahr ist, falls der Beschleunigungssensor registriert wurde, also die Aufnahme gestartet wurde
    private boolean registered;
    // gibt den Startzeitpunkt eines Abschnittes an
    private double time1;
    // gibt den aktuellen Zeitpunkt an
    private double time2;
    // gibt die Anzahl an Messungen innerhalb eines Segments an, damit der Durchschnitt des Iris bestimmt werden kann
    private int anzahlMessungen;
    // gibt den Zeitabstand vom Startzeitpunkt zur aktuellen Zeit an
    private double deltaT;
    // gibt den aktuellen Beschleunigungswert an
    private double acceleration;
    // gibt den aktuellen Iri Wert an
    private double IRI;
    // gibt die Summe aller Iriwerte in einem Segment an (um den Durchschnitt zu bestimmen)
    private double IRISumme;
    // gibt die bislang zurückgelegte Strecke an
    private double zurückgelegteStrecke;
    // gibt an, ob die aktuelle Route schon gespeichert wurde
    private boolean safed = false;
    // Straßenlinie erstellen
    private Polyline strassenLinie; // Straßenlinie erstellen, die alle Punkte einer Straße enthält
    // Vermutlicher Untergrund der Straße
    private String untergrund;

    // Permission Request Code
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    // LocationListener zur Bestimmung der Userposition
    private final LocationListener locationListener = new LocationListener() {
        // hier wird basierend auf der Geocodierung die Karte mit einer neuen Linie geupdated
        @Override
        public void onLocationChanged(Location location) {
            // Linie erstellen
            Polyline line = new Polyline(map);

            // nur die Linie zeichnen, wenn auf Start gedrückt wurde
            if (registered) {
                // ErsterPunkt
                line.addPoint(userPosMarker.getPosition());

                // Zurückgelegte Strecke in Metern bestimmen (auf dem Kreisbogen)
                double radius = 6371;

                double x1 = radius * Math.cos(userPosMarker.getPosition().getLatitude()) * Math.cos(userPosMarker.getPosition().getLongitude());
                double y1 = radius * Math.cos(userPosMarker.getPosition().getLatitude()) * Math.sin(userPosMarker.getPosition().getLongitude());
                double z1 = radius * Math.sin(userPosMarker.getPosition().getLatitude());

                double x2 = radius * Math.cos(location.getLatitude()) * Math.cos(location.getLongitude());
                double y2 = radius * Math.cos(location.getLatitude()) * Math.sin(location.getLongitude());
                double z2 = radius * Math.sin(location.getLatitude());

                double alpha = Math.acos((x1 * x2 + y1 * y2 + z1 * z2) / (radius * radius));

                zurückgelegteStrecke = alpha * radius * 1000;
            }
            String alteStrasse = strasse;

            // neue Strasse herausfinden
            String[] s;

            // falls keine Internetverbindung: neue Straße kann nicht bestimmt werden, alteStrasse wird weiterhin als die letzte bekannte Position angenommen
            s = getStrasse(location);

            if (s == null){
                //neue Strasse konnte nicht bestimmt werden, also bleibt die alte Strasse aktuell
                s = new String[2];
                s[0] = alteStrasse;
                s[1] = "";
            }

            // aktuelle neue Position dem Segment hinzufügen
            aktuSeg.add(new GeoPoint(location.getLatitude(), location.getLongitude()));

            //falls neues Segment begonnen hat
            if(!s[0].equals(alteStrasse)) {

                // aktuelles Segment wird in die Liste aus Segmenten eingefügt
                if (registered){
                    abgefahreneSegmente.add(aktuSeg);

                    //falls kein Messwert im aktuellen Segment: IRI kann nicht bestimmt werden und wird mit Wertt -1 gespeichert (graue Linie, siehe unten)
                    if(anzahlMessungen == 0){
                        IRI = -1.0;
                    }
                    iriwerte.add(IRI);

                    strassenLinie.addPoint(userPosMarker.getPosition());

                    // Straße/ Segment ist zuende: Straßenlinie wird eingefärbt und gezeichnet

                    int [] farbwerte = getColor(iriwerte.get(iriwerte.size() - 1));
                    strassenLinie.setColor(Color.rgb(farbwerte[0], farbwerte[1], farbwerte[2]));
                    strassenLinie.setTitle("Vermutung: " + untergrund);
                    strassenLinie.setSnippet("IRI: " + Double.toString(IRI));

                    // eingefärbte Strassenlinie zur Karte hinzufügen
                    map.getOverlays().add(strassenLinie);

                    // schwarze, alte Linien aus der Karte entfernen
                    for (int i = 0; i < aktuSeg.size(); i++){
                        map.getOverlays().remove(aktuSeg.get(i));
                    }

                    // Parameter zurücksetzen für neuen Streckenabschnitt
                    zurückgelegteStrecke = 0.0;
                    IRI = 0.0;
                    IRISumme = 0.0;
                    anzahlMessungen = 0;
                    time1 = System.currentTimeMillis();
                    time2 = System.currentTimeMillis();
                    strassenLinie = new Polyline(map);
                    strassenLinie.addPoint(userPosMarker.getPosition());
                }

                // wenn die Strassennamen unterschiedlich sind, muss ein neues Segment beginnen
                aktuSeg = new ArrayList<GeoPoint>();
                aktuSeg.add(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }

            // alten Marker entfernen
            map.getOverlays().remove(userPosMarker);

            // neuen Marker setzen
            createUserPosMarker(location, s[0], s[1]);

            // neue Markerposition zur Straße hinzufügen
            strassenLinie.addPoint(userPosMarker.getPosition());

            if(registered) {
                // zweiterPunkt
                line.addPoint(userPosMarker.getPosition());
            }

            // neue Linie zur Karte hinzufügen
            if (registered) {
                // Linie zum Layout hinzufügen
                map.getOverlays().add(line);
            }

            // neu zeichnen bzw. updaten der MapView
            map.invalidate();

            // 2 Modi:
            // Zentriert: Die Bounding Box bleibt ständig auf dem Nutzer, während er sich bewegt
            // Dezentriert: Der User kann die Karte beliebig verschieben und sich die Umgebung anschauen, ohne dabei den Kartenrahmen zu verlassen und die Karte unendlich weiter zu schieben
            // Wechsel zwischen den Modi durch den Zentriert Button möglich
            if (zentriert) {
                //map.getController().animateTo(userPosMarker.getPosition());
                map.getController().setCenter(userPosMarker.getPosition());
                map.getController().setZoom(17.96);
            }
        }
    };


    // - Methoden ---------------------------------------------------------------------------------
    /**
     * Beim Erstellen des Menüs wird in dieser Methode festgelegt, dass die menu.xml Datei
     * als Layout für das Menü verwendet werden. Diese Datei bestimmt, welche Items in dem
     * Menü angezeigt werden.
     * @param menu das Menü
     * @return true (true = Menü anzeigen lassen, false = Menü nicht anzeigen lassen)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    /**
     * In dieser Methode wird bestimmt, was passiert, wenn der User ein
     * bestimmtes Item angeklickt hat.
     * @param item das ausgewählte Item des Menüs
     * @return welches Item angeklickt wurde
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.help) {
            // Toast.makeText(this, "Hilfe wurde angeklickt", Toast.LENGTH_SHORT).show();
            // Layout aufbauen
            LayoutInflater factory = LayoutInflater.from(getApplicationContext());
            View inf = factory.inflate(R.layout.info, null);

            // Dialog, der darüber informiert, wie die App verwendet werden muss
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bedienungsanleitung")
                    .setView(inf)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else if (item.getItemId() == R.id.mosa) {
            // Layout aufbauen
            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
            View inf = factory.inflate(R.layout.denied, null);
            //View inf = factory.inflate(R.layout.mosasaurus, null);

            // Video einbinden
            /*
            VideoView vid = inf.findViewById(R.id.video_view);
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mosa);
            vid.setVideoURI(videoUri);

            // Progressbar
            ProgressBar prog = inf.findViewById(R.id.Progressbar);
            prog.setProgress(0);
            prog.setMax(100);

            // Video Timer für die Progressbar
            vidTimer vidtimer = new vidTimer(vid, prog, 67);

            // play Button
            Button play = inf.findViewById(R.id.play);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vid.start();
                    vidtimer.start();
                }
            });

            // stop Button
            Button stop = inf.findViewById(R.id.stop);
            stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vid.pause();
                    vidtimer.stop();
                }
            });
            */
            // Dialog, der über den Mosa informiert
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Der Mosasaurus war ein niedlicher Meeressaurier," +
                    " der während der Oberkreide im Atlantischen Ozean lebte. Er stand an" +
                    " der Spitze der Nahrungskette und wurde bis zu 18 Meter lang und 14 Tonnen schwer.\n\n" +
                    //" Außerdem erhielt der Mosasaurus die \"Hauptrolle\" in Jurassic World.\n" +
                    "Der Film wurde in der öffentlichen Version aus Lizenzgründen entfernt.\n")
                    .setTitle("RAWRRR GRRRR")
                    .setView(inf)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            try {
                                //vidtimer.stop();
                            }
                            catch (Exception e) {
                                // timer war bereits gestoppt
                            }
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else if(item.getItemId() == R.id.film) {
            // Layout aufbauen
            LayoutInflater factory = LayoutInflater.from(MainActivity.this);
            View inf = factory.inflate(R.layout.mosasaurus, null);

            // Video einbinden
            VideoView vid = inf.findViewById(R.id.video_view);
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.filmapp);
            vid.setVideoURI(videoUri);

            // Progressbar setzen: anpassen, da der Film länger ist als der Mosafilm
            ProgressBar prog = inf.findViewById(R.id.Progressbar);
            prog.setProgress(0);
            prog.setMax(100);

            // Video Timer für die Progressbar
            vidTimer vidtimer = new vidTimer(vid, prog, 97);

            // play Button
            Button play = inf.findViewById(R.id.play);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vid.start();
                    vidtimer.start();
                }
            });

            // stop Button
            Button stop = inf.findViewById(R.id.stop);
            stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vid.pause();
                    vidtimer.stop();
                }
            });

            // Dialog, der über den Mosa informiert
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("In diesem kurzen Einführungsfilm werden die grundlegenden Funktionen der App sowie ihre korrekte Nutzung erläutert.\n")
                    .setTitle("Einführungsfilm")
                    .setView(inf)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            try {
                                vidtimer.stop();
                            } catch (Exception e) {
                                // timer war bereits gestoppt
                            }
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Methode zum Öffnen des Exit Dialogfensters, welches zur Beendigung der App führt.
     */
    public void openExitDialog() {
        ExitDialog exitDialog = new ExitDialog();
        exitDialog.show(getSupportFragmentManager(), "exit dialog");
        exitDialog.setCancelable(false);
    }


    /**
     * Print Methode, da zu viel Python in meinem Kopf ist.
     * @param o das zu printende Objekt
     */
    public void print(Object o) {
        System.out.println(o);
    }


    /**
     * Methode um Location Updates zu Aktivieren.
     */
    public void initLocationListener() {
        // - Location Updates erhalten ----------------------------------------------------
        // Referenzvariable auf einen Location Manager erhalten
        LocationManager locationManager = (LocationManager) getSystemService(ctx.LOCATION_SERVICE);
        // GPS als Location Provider auswählen, geupdated wird alle 1 Sekunde mit einer minmal notwendigen Bewegungsdistanz von 10 Metern.
        // Das Update geht an die onLocationChanged Callbackfunktion des zu Beginn erstellten locationListener.
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
    }


    /**
     * Methode für die Erstellung des Markers für den Benutzerstandort
     * @param location Standortangabe
     * @param street Strasse des Standortes
     * @param addresse volle Addresse des Standortes
     */
    public void createUserPosMarker(Location location, String street, String addresse) {
        // Letzte bekannte Position wurde ermittelt. Achtung: diese Position könnte auch Null sein!
        if (location != null) {
            // Geopoint erzeugen
            GeoPoint userPosition = new GeoPoint(location.getLatitude(), location.getLongitude());

            // Marker der Karte hinzufügen
            this.userPosMarker = new Marker(map);
            this.userPosMarker.setIcon(ctx.getResources().getDrawable(R.drawable.dino4));
            this.userPosMarker.setPosition(userPosition);

            // AnchorU höherer Wert = weiter nach links; AnchorV höherer Wert = weiter unten
            this.userPosMarker.setAnchor(0.155f, 0.21f);
            // Info Window gestalten
            this.userPosMarker.setInfoWindowAnchor(0.155f, 0.21f);
            this.userPosMarker.setTitle("RAWRR GRRR - Dein Standort");
            this.userPosMarker.setSnippet("Geo. Koordinaten: " + Math.round(location.getLatitude() * 10000) / 10000.0 + " | " +Math.round(location.getLongitude() * 10000) / 10000.0);
            this.strasse = street;
            this.userPosMarker.setSubDescription(addresse);
            this.map.getOverlays().add(userPosMarker);
            this.map.invalidate();
        }
    }


    /**
     * Straßennamen von der Location herausfinden
     * @param location Standortangabe
     * @return String Array: an erster Stelle steht die Strasse und an zweiter die gesamte Adresse
     */
    public String[] getStrasse(@NonNull Location location) {
        // Geocoder Provider fragen auf welcher Straße wir sind
        try {
            Address a = (Address) geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
            // Hausnummer Straßenname, Ort, PLZ, Land?
            String str = a.getAddressLine(0);
            // Adresse nach Kommas aufteilen, nur den ersten Part behalten
            str = str.split(",")[0];
            // prüfen ob eine Hausnummer dem Strassennamen vorangeht: es gibt nämlich auch Straßen ohne Hausnummern!
            Pattern pattern = Pattern.compile("^[0-9]]");
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()){
                // wenn ja, dann die Hausnummer vorne abschneiden
                str = str.split(" ", 2)[1];
            }
            // Strassenname und komplette Adresse zurückgeben
            return new String[]{str, a.getAddressLine(0)};
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Sorry, der Straßenname konnte nicht ermittelt werden!");
            return null;
        } catch (Exception e) {
            System.err.println("Sorry, beim Ermitteln des Straßennamens ist ein Fehler aufgetreten!");
            return null;
        }
    }


    /**
     * Methode zum Bestimmen der Farbe der Linie
     * @param iriwert Aktueller Iriwert, aus dem die Fareb für das Segment berechnet werden soll
     * @return Int Array, in dem die drei Werte für die RGB Farbkomponenten gespeichert werden
     */
    public int[] getColor(double iriwert){
        // Linienfarbe setzen auf Basis der Iriwerte und Klassen im Paper
        // Neuer Ansatz über die Klasseneinteilungen
        int [] farben = new int [3];

        // falls kein Messwert im aktuellen Segment genommen wurde (Zeit, in der das Segment erstellt wurde, liegt unter einer Sekunde, Sensor hatte einen
        // Aussetzer, ...) wird die Linie grau gefärbt und das Info Window entsprechend angepasst1
        if(iriwert == -1.0){
            farben[0] = 128;
            farben[1] = 128;
            farben[2] = 128;
            untergrund = "Kein Messpunkt in diesem Segment";
        }

        else if(iriwert <= 3.0){
            farben [0] = 0;
            farben [1] = 255;
            farben [2] = 0;
            untergrund = "Glatter Weg";
        }
        else if(iriwert <= 6.0){
            farben [0] = 50;
            farben [1] = 200;
            farben [2] = 0;
            untergrund = "Rauer Weg";
        }
        else if(iriwert <= 9.0){
            farben [0] = 238;
            farben [1] = 250;
            farben [2] = 0;
            untergrund = "Beschädigter Weg";
        }
        else if(iriwert <= 15.0){
            farben [0] = 200;
            farben [1] = 50;
            farben [2] = 0;
            untergrund = "Kopfsteinpflaster/ unbefestigter Weg";
        }
        else{
            farben [0] = 255;
            farben [1] = 0;
            farben [2] = 0;
            untergrund = "Nur zugänglich für Dinos";
        }
        return farben;
    }


    /**
     * Methode zum Lesen der ausgewählten Datei.
     * @param dateiname Dateiname, der eingelesen werden soll
     * @return Byte Array, in welchem sich die zu lesende Datei befindet
     */
    public byte[] getBytesFromFiles(String dateiname) {
        FileInputStream in = null;
        try{
            // durch den InputStream die durch den uri angegebene Datei einlesen
            in = ctx.openFileInput(dateiname);
            // der Inhalt der Datei soll in ein Byte Array geschrieben werden, dazu wird ein ByteArrayOutputStream benötigt
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            // leeren Buffer mit einer Größe von 1024 Bytes erstellen
            byte [] buffer = new byte[1];
            // Bytes der Datei solange lesen, bis die Datei zu Ende ist, Klammer um len = in.read(buffer) ist wichtig!
            int len = 0;
            while((len = in.read(buffer)) != -1) {
                // gelesene Bytes in das Byte Array schreiben
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Sorry, die ausgewählte Datei konnte nicht eingelesen werden!");
        }
        return null;
    }


    /** Überschreiben der Callback Methode onRequestPermissionsResult.
     * Diese Methode wird automatisch vom System im Hintergrund aufgerufen, nachdem requestPermissions() ausgeführt wurde.
     * Sie enthält dann die Ergebnisse des Dialogs mit dem User.
     * @param requestCode enthält den zuvor festgelegten Requestcode der jeweiligen Abfrage
     * @param permissions ist das String Array, in welchem sich die verschiedenen Permissions befinden
     * @param grantResults ist ein int Array, welches die Ergebnisse der Abfrage beinhaltet
     * grantResults: mögliche Werte sind 0 (= erlaubt) oder -1 (= verweigert): PackageManager.PERMISSION_GRANTED (= 0) oder PackageManager.PERMISSION_DENIED (= -1)
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Parent Methode aufrufen
        // Prüfen, ob die Permission für die FINE Location im Dialog von dem User erteilt wurde
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults[0] == 0)  { // Permission wurde erteilt
            // dieser Check ist eigentlich sinnlos, da eine Zeile darüber bereits geprüft wurde, ob die Permission ertweilt wurde.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            // die getLastLocation() Methode wird aufgerufen und gibt ein Object vom Typ Task zurück
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Updates aktivieren und Standort markieren
                    initLocationListener();

                    //falls/solange Standort nicht direkt geladen werden kann
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setMessage("Die aktuelle Position wird bestimmt.")
                            .setTitle("Positionsbestimmung");
                    AlertDialog dialog = builder.create();

                    while(location == null){
                        dialog.show();
                    }
                    dialog.hide();

                    //Location wird geholt
                    String[] s = getStrasse(location);
                    createUserPosMarker(location, s[0], s[1]);
                    map.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    map.getController().setZoom(17.96);
                }
            });
        }
        // die permission für die Fine Location wurde nicht gewährt oder es wurde die Coarse location gewährt
        else {
            // Prüfung, ob die Location permanent abgelehnt wurde:
            // falls Nein: Dialog zeigen, dass diese Permission benötigt wird
            // falls Ja: Beendigung des Programms
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Dialog, der über die Notwendigkeit des Location Zugriffs informiert
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage("Die App benötigt diese Erlaubnis, um den genauen Standort des Benutzers bestimmten zu können.")
                        .setTitle("Erlaubnis für Standortabfrage");
                builder.setCancelable(false);

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
                    }
                });
                builder.setNegativeButton("Ablehnen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Benutzer verweigert die Erteilung der Permission, daher Beendigung des Programms durch Exit Dialog
                        openExitDialog();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                // never ask again wurde ausgewählt => Funktionalität kann nicht gegeben werden, daher Beendigung des Programms durch Exit Dialog
                openExitDialog();
            }
        }
    }


    /**
     * onResume Methode des Android onResume/onPause Lifecycles
     */
    public void onResume(){
        super.onResume();
        // aktualisieren bzw. wieder aufnehmen der osmdroid Konfiguration
        map.onResume();
    }


    /**
     * onPause Methode des Android onResume/onPause Lifecycles
     */
    public void onPause(){
        super.onPause();
        // pausieren der osmdroid Konfiguration
        map.onPause();
    }


    /** "Mainmethode"/Initialisierung */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // zu verwendende Layouts festlegen
        setContentView(R.layout.activity_main2);
        // für die Actionbar! Nullcheck ist zwar redundant, allerdings wird ohne ihn ein Fehler geworfen
        getSupportActionBar().setDisplayOptions(Objects.requireNonNull(ActionBar.DISPLAY_SHOW_CUSTOM));
        getSupportActionBar().setCustomView(R.layout.action_bar_layout);

        // Klassenvariablen initialisieren
        this.map = null;
        this.userPosMarker = null;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.ctx = getApplicationContext();
        this.geo = new Geocoder(ctx);
        this.strasse = null;
        this.aktuSeg = new ArrayList<GeoPoint>();
        this.abgefahreneSegmente = new ArrayList<ArrayList<GeoPoint>>();
        this.zentriert = true;
        this.registered = false;
        this.time1 = System.currentTimeMillis();
        this.time2 = 0.0;
        this.anzahlMessungen = 0;
        this.deltaT = 0.0;
        this.acceleration = 0.0;
        this.IRI = 0.0;
        this.IRISumme = 0.0;
        this.iriwerte = new ArrayList<Double>();
        this.safed = false;
        this.zurückgelegteStrecke = 0.0;
        this.strassenLinie = new Polyline(map);



        // Farben bestimmen
        int colorButtons = ContextCompat.getColor(ctx, R.color.teal_700);
        int colorButtons2 = ContextCompat.getColor(ctx, R.color.teal_200);

        // Elemente der App holen
        Button startbutton = findViewById(R.id.startButton);
        Button endebutton = findViewById(R.id.endeButton);
        TextView textIRI = findViewById(R.id.textIRI);
        Button zentrierung = findViewById(R.id.btn);
        Button speichern = findViewById(R.id.speichern);
        Button laden = findViewById(R.id.zentrierung);
        Button legende = findViewById(R.id.btn2);

        // Farben anwenden
        startbutton.setBackgroundColor(colorButtons2);
        endebutton.setBackgroundColor(colorButtons);
        speichern.setBackgroundColor(colorButtons);
        laden.setBackgroundColor(colorButtons2);
        endebutton.setEnabled(false);
        speichern.setEnabled(false);
        speichern.setVisibility(View.VISIBLE);
        zentrierung.setBackgroundColor(ContextCompat.getColor(ctx, R.color.blue));
        zentrierung.setTextColor(Color.WHITE);
        legende.setBackgroundColor(ContextCompat.getColor(ctx, R.color.blue));
        legende.setTextColor(Color.WHITE);



        // - Permissionsabfrage & Positionsbestimmung & Speicher ----------------------------------
        // Prüfen ob der Google Play Services verfügbar sind. Mögliche Werte sind: ConnectionResult.SUCCESS (=0), ConnectionResult.SERVICE_MISSING (=1),
        // ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED (=2) oder ConnectionResult.SERVICE_DISABLED (=3)
        int googlePlayStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (googlePlayStatus == ConnectionResult.SUCCESS) {
            // Fall 1: Standort Permission wurde nicht erlaubt
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
            // Fall 2: Standort Permission wurde bereits erlaubt
            else{
                this.fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Standort Updates aktivieren und Standort markieren
                        initLocationListener();

                        // falls/solange Standort nicht direkt geladen werden kann, soll eine Info Message ausgegeben werden
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setMessage("Die aktuelle Position wird bestimmt.")
                                .setTitle("Positionsbestimmung");
                        AlertDialog dialog = builder.create();

                        while(location == null){
                            dialog.show();
                        }
                        dialog.hide();

                        // Location wird geholt
                        String[] s = getStrasse(location);
                        // falls das Bestimmen der Adresse gescheitert ist, wird für den Marker keine Strasse und keine Adresse gesetzt
                        if (s == null){
                            createUserPosMarker(location,"","");
                        }
                        else {
                            createUserPosMarker(location, s[0], s[1]);
                        }
                        map.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
                        map.getController().setZoom(17.96);
                    }
                });
            }
        } else { // GooglePlayServices sind nicht verfügbar, daher Beendigung des Programms durch Exit Dialog
            openExitDialog();
        }

        // - Accelerometer ----------------------------------------------
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerVertical = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        SensorEventListener sel = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // IRI-Parameter berechnen
                time2 = System.currentTimeMillis();
                deltaT = (time2 - time1) / 1000;

                if (deltaT > 1.0) { // jede Sekunde eine Messung
                    anzahlMessungen++;
                    acceleration = sensorEvent.values[1]; //pitch (values[1]) wird benötigt um schlaglöcher zu detektieren

                    // IRI bestimmen
                    IRISumme += 0.5 * Math.abs(acceleration) * (deltaT * deltaT); // Einheit: Meter
                    IRI = IRISumme / anzahlMessungen;

                    // NAN Werte abfangen, falls wir uns aktuell nicht bewegen
                    if(zurückgelegteStrecke != 0) {
                        IRI = IRI / (zurückgelegteStrecke / 1000); //Einheit: Meter pro Kilometer
                    }
                    else{
                        IRI = 0.0;
                    }

                    // Textfeld auf den aktuellen IRI setzen
                    textIRI.setText("Aktueller gemittelter IRI Wert: " + Double.toString(IRI));

                    // Neue Messung beginnen
                    time1 = System.currentTimeMillis();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        // - Startbutton --------------------------------------------------
        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!registered && userPosMarker.getPosition()!= null) {
                    // alte Route aus der Karte löschen
                    map.getOverlays().clear();
                    // Scalebar wieder hinzufügen
                    ScaleBarOverlay massstab = new ScaleBarOverlay(map);
                    massstab.setTextSize(22.0F);
                    map.getOverlays().add(massstab);

                    map.getOverlays().add(userPosMarker);

                    sm.registerListener(sel, accelerometerVertical, SensorManager.SENSOR_DELAY_NORMAL);
                    endebutton.setBackgroundColor(colorButtons2);
                    startbutton.setBackgroundColor(colorButtons);
                    startbutton.setEnabled(false);
                    endebutton.setEnabled(true);
                    speichern.setEnabled(false);
                    laden.setEnabled(false);
                    laden.setBackgroundColor(colorButtons);
                    safed = false;
                    time1 = System.currentTimeMillis();
                    registered = true;

                    // Listen zurücksetzen und neu initialisieren
                    abgefahreneSegmente = new ArrayList<ArrayList<GeoPoint>>();
                    strassenLinie = new Polyline();
                    strassenLinie.addPoint(userPosMarker.getPosition());
                    aktuSeg = new ArrayList<GeoPoint>();
                    aktuSeg.add(new GeoPoint(userPosMarker.getPosition().getLatitude(), userPosMarker.getPosition().getLongitude()));
                    iriwerte = new ArrayList<Double>();
                }
            }
        });

        // - Endebutton --------------------------------------------------
        endebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (registered) {
                    // aktuelles Segment wird in die Liste aus Segmenten eingefügt
                    abgefahreneSegmente.add(aktuSeg);
                    iriwerte.add(IRI);
                    strassenLinie.addPoint(userPosMarker.getPosition());

                    int [] farbwerte = getColor(iriwerte.get(iriwerte.size() - 1));
                    strassenLinie.setColor(Color.rgb(farbwerte[0], farbwerte[1], farbwerte[2]));
                    strassenLinie.setTitle("Vermutung: " + untergrund);
                    strassenLinie.setSnippet("IRI: " + Double.toString(IRI));

                    // eingefärbte Strassenlinie zur Karte hinzufügen
                    map.getOverlays().add(strassenLinie);

                    // schwarze, alte Linien aus der Karte entfernen
                    for (int i = 0; i < aktuSeg.size(); i++){
                        map.getOverlays().remove(aktuSeg.get(i));
                    }

                    // Segmente beenden und Werte reseten
                    aktuSeg = new ArrayList<GeoPoint>();
                    strassenLinie = new Polyline();
                    sm.unregisterListener(sel);
                    startbutton.setBackgroundColor(colorButtons2);
                    endebutton.setBackgroundColor(colorButtons);
                    endebutton.setEnabled(false);
                    startbutton.setEnabled(true);
                    speichern.setEnabled(true);
                    speichern.setBackgroundColor(colorButtons2);
                    laden.setEnabled(true);
                    laden.setBackgroundColor(colorButtons2);
                    registered = false;
                    textIRI.setText(R.string.ende1);
                }
            }
        });

        // - Zentrierungsbutton --------------------------------------------------
        zentrierung.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (zentriert) {
                    zentriert = false;
                    zentrierung.setText("Zentrierung Aus");
                } else {
                    zentriert = true;
                    zentrierung.setText("Zentrierung An");
                    map.getController().animateTo(userPosMarker.getPosition());
                    map.getController().setZoom(17.96);
                }
            }
        });

        // - Route Speichern Button --------------------------------------------------
        speichern.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                // Prüfen, ob schon gespeichert wurde.
                // Falls nein: schreibe die Liste an Polylines sowie die zugehörigen IRI Werte in eine Datei
                // Falls ja: nicht nochmal speichern

                if(!safed) {
                    // Dialog zum Speichern erstellen
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage("Bitte geben Sie einen Dateinamen ein: ");
                    builder.setTitle("Dateinamen festlegen");
                    builder.setCancelable(true);

                    // Edit Text Feld erstellen und einfügen
                    EditText editText = new EditText(ctx);
                    editText.setHint("Dateiname");
                    editText.setBackgroundColor(Color.LTGRAY);
                    builder.setView(editText);

                    // Positive Button
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Dateiname auslesen
                            String filename = editText.getText().toString();
                            filename = filename + ".txt";

                            // Daten in die Datei schreiben
                            try (FileOutputStream fos = ctx.openFileOutput(filename, Context.MODE_PRIVATE)) {

                                for(int i = 0; i < iriwerte.size(); i++){
                                    String zeile = "";

                                    // Iriwert schreiben
                                    zeile += iriwerte.get(i).toString();

                                    // Geopunkte schreiben (ArrayList in ArrayList, also für jedes Segment eine Array List)
                                    for(int e = 0; e < abgefahreneSegmente.get(i).size(); e++){
                                        zeile += ";";
                                        zeile += Double.toString(abgefahreneSegmente.get(i).get(e).getLatitude());
                                        zeile += ",";
                                        zeile += Double.toString(abgefahreneSegmente.get(i).get(e).getLongitude());
                                    }
                                    //Gesamte Zeile schreiben
                                    fos.write(zeile.getBytes());

                                    // Zeilenumbruch für die nächste Zeile
                                    fos.write("\n".getBytes());
                                }

                                //"/storage/emulated/0/Android/data/com.example.test/files/" +
                                // hier wird kein fos.close() benötigt, weil der fileoutputstream im kopf des try blockes erstellt wird
                                safed = true;
                                textIRI.setText("Der Weg und die IRI Werte wurden erfolgreich gespeichert!");

                            } catch (Exception e) {
                                e.printStackTrace();
                                print("Das Schreiben der Datei hat nicht geklappt!");
                            }
                        }
                    });
                    // negative Button: einfach Abbruch und nichts geschieht
                    builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            print("Nichts passiert, Abbruch!");
                        }
                    });
                    // Speichern Dialog erstellen und anzeigen
                    AlertDialog speichernDialog = builder.create();
                    speichernDialog.show();
                }
                else{
                    textIRI.setText("Der aktuelle Weg und die IRI Werte wurden bereits gespeichert!");
                }
            }
        });

        // - Route Laden Button --------------------------------------------------
        laden.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Route öffnen");

                // Single Choice Liste anlegen
                File[] liste = ctx.getFilesDir().listFiles();
                ArrayList <String> listeNurTXT = new ArrayList<String>();

                // Filter, dass der User beim Laden später nur die Dateien mit .txt am Ende auswählen kann: an Stelle 0 liegt die osm droid karte
                for(int i = 1; i < ctx.getFilesDir().list().length; i++){
                    listeNurTXT.add(liste[i].getName());
                }

                String [] dateinamen = new String [listeNurTXT.size()];

                for(int i = 0; i < listeNurTXT.size(); i++){
                    dateinamen[i] = listeNurTXT.get(i);
                }

                builder.setSingleChoiceItems(dateinamen, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int datei) {
                        dialogInterface.dismiss();
                        // beim KLick des Dateinamens soll diese Datei geöffnet werden.

                        // Byte Array mit dem Inhalt der eingelesenen Datei initialisieren
                        byte[] inhalt = getBytesFromFiles(dateinamen[datei]);
                        String s = new String(inhalt, StandardCharsets.UTF_8);

                        // Hilfsvariablen
                        double iri = -1.0;
                        double lat = -999.0;
                        double lon = -999.0;
                        Polyline line = null;

                        // eine Zeile ist ein Segment, also den String erstmal in Segmente aufteilen
                        String[] segmente = s.split(System.lineSeparator());
                        // über die Segmente iterieren, jedes Segment stellt eine Polyline mit mehreren Punkten dar
                        for (int i = 0; i < segmente.length; i++) {
                            line = new Polyline(map);
                            // Iri und Punkte sind durch ; getrennt
                            String[] werte = segmente[i].split(";");
                            // über die Werte iterieren
                            for (int j = 0; j < werte.length; j++) {
                                // an erster Stelle in einer Zeile steht immer der Iri
                                if (j == 0) {
                                    iri = Double.parseDouble(werte[0]);
                                } else {
                                    // an erster Stelle steht immer die Latitude und an zweiter Stelle die Longitude
                                    String[] koords = werte[j].split(",");
                                    lat = Double.parseDouble(koords[0]);
                                    lon = Double.parseDouble(koords[1]);
                                    // Punkt der Polyline hinzufügen
                                    line.addPoint(new GeoPoint(lat, lon));
                                }
                            }
                            // Polyline einfärben und zur Karte hinzufügen
                            int[] help = getColor(iri);
                            line.setColor(Color.rgb(help[0], help[1], help[2]));
                            line.setTitle("Vermutung: " + untergrund);
                            line.setSnippet("IRI: " + Double.toString(iri));
                            map.getOverlays().add(line);
                            map.invalidate();
                        }
                    }
                });
                // Laden Dialog erstellen und anzeigen
                AlertDialog ladenDialog = builder.create();
                ladenDialog.show();
            }
        });

        // - Legende Button --------------------------------------------------
        legende.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Legende");
                LayoutInflater factory = LayoutInflater.from(view.getContext());
                View legendenview = factory.inflate(R.layout.legendenview, null);
                builder.setView(legendenview);
                builder.setNeutralButton("Ok", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog legendenDialog = builder.create();
                legendenDialog.show();
            }
        });


        // - Karte ---------------------------------------------------------------------
        // Initialisierung der osmdroid Konfiguration
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Erstellung der Karte
        this.map = (MapView) findViewById(R.id.theMap);
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.map.setMultiTouchControls(true);
        this.map.setBuiltInZoomControls(true);
        this.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        this.map.setHorizontalMapRepetitionEnabled(false);
        this.map.setVerticalMapRepetitionEnabled(false);
        this.map.setMinZoomLevel(2.5);
        // Karte auf ein Tile beschränken
        this.map.setScrollableAreaLimitLatitude(MapView.getTileSystem().getMaxLatitude(), MapView.getTileSystem().getMinLatitude(), 0);
        this.map.setScrollableAreaLimitLongitude(MapView.getTileSystem().getMinLongitude(), MapView.getTileSystem().getMaxLongitude(), 0);
        // Massstab hinzufügen
        ScaleBarOverlay massstab = new ScaleBarOverlay(this.map);
        massstab.setTextSize(22.0F);
        this.map.getOverlays().add(massstab);
    }
}
