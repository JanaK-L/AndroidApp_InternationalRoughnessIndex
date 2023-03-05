package com.example.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatDialogFragment;

public class ExitDialog extends AppCompatDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // eigenen neuen Dialog erstellen
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Titel und Nachricht des Dialogfensters setzen
        builder.setTitle("Information");

        // dem Benutzer mit einem super süßen Babydino Bild ein schlechtes Gewissen machen
        LayoutInflater factory = LayoutInflater.from(getActivity());
        View inf = factory.inflate(R.layout.denied, null);
        builder.setView(inf);

        // builder.setIcon(R.drawable.saddino);
        //ImageView image = new ImageView(getActivity());
        //image.setImageURI(Uri.parse("android.resource://" + getActivity().getPackageName() + "/drawable/" +R.drawable.saddino));

        builder.setMessage("Die benötigten Funktionen dürfen nicht verwendet werden, weshalb die Applikation beendet wird. Dies kann in den Einstellungen rückgängig gemacht werden.");
        // Button dem Dialogfenster hinzufügen
        builder.setPositiveButton("Beende App", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        });
        // erstellten Dialog zurückgeben
        return builder.create();
    }
}
