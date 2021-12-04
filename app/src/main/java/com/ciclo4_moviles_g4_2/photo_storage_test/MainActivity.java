package com.ciclo4_moviles_g4_2.photo_storage_test;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final String RUTA_STORAGE = "gs://pappseando-a879c.appspot.com/";

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ImageView downloadedImgView;
    private ProgressDialog progressMsg;
    private String rutaImg;
    private String currentPhotoPath;
    //private Uri uriPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLocalImage = findViewById(R.id.btn_upload_image);
        Button btnDownload = findViewById(R.id.btn_download);
        Button btnPhotoShot = findViewById(R.id.btn_upload_photo);
        downloadedImgView = findViewById(R.id.iv_downloaded_image);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        progressMsg = new ProgressDialog(this);

        btnLocalImage.setOnClickListener(v -> {
            //Abre el gestor de imagenes locales para permitir elegir una de ellas

            Intent intentGallery = new Intent(Intent.ACTION_PICK);
            intentGallery.setType("image/*");
            startActivityForResult(intentGallery, 1);
        });

        btnPhotoShot.setOnClickListener(v -> {
            //Abre el gestor de cámara para sacar una foto
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    currentPhotoPath = photoFile.getAbsolutePath();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri uriPhoto = FileProvider.getUriForFile(this,
                            "com.ciclo4_moviles_g4_2.photo_storage_test.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriPhoto);
                    startActivityForResult(takePictureIntent, 2);
                }
            }
        });

        btnDownload.setOnClickListener(v -> {
            //Obtiene la url de la imagen desde Firebase Storage y la muestra en el ImageView
            if (rutaImg != null) {
                StorageReference urlRef = storage.getReferenceFromUrl(RUTA_STORAGE + rutaImg);

                urlRef.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(this)
                        .load(uri)
                        .fitCenter()
                        .centerCrop()
                        //.circleCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Toast.makeText(getApplicationContext(), "Se ha cargado el archivo '" + uri.getLastPathSegment().split("/")[1] + "' desde el storage correctamente", Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        })
                        .into(downloadedImgView))
                        .addOnFailureListener(exception -> {
                        });
            } else {
                Glide.with(this)
                        .load(R.mipmap.ic_launcher)
                        .into(downloadedImgView);
                Toast.makeText(getApplicationContext(), "No hay imágenes disponibles para mostrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == -1) {
            if (requestCode == 1) {
                //Carga la imagen desde un archivo local a Firebase Storage
                Uri uriLocal = data != null ? data.getData() : null;

                if (uriLocal != null) {
                    rutaImg = "image/localImage";
                    StorageReference photoRef = storageRef.child(rutaImg);

                    loadProgressMessage();
                    photoRef.putFile(uriLocal).addOnSuccessListener(taskSnapshot -> {
                        progressMsg.dismiss();
                        Toast.makeText(getApplicationContext(), "Se ha subido la imagen al storage correctamente", Toast.LENGTH_SHORT).show();
                    });
                } else
                    Toast.makeText(getApplicationContext(), "No se ha podido subir la imagen al storage", Toast.LENGTH_SHORT).show();
            } else if (requestCode == 2) {
                //Carga la imagen tomada desde la cámara a Firebase Storage
                if (currentPhotoPath != null) {
                    rutaImg = "image/photoImage";
                    StorageReference photoRef = storageRef.child(rutaImg);

                    loadProgressMessage();
                    photoRef.putBytes(convertImageToByteArray(currentPhotoPath)).addOnSuccessListener(taskSnapshot -> {
                        progressMsg.dismiss();
                        Toast.makeText(getApplicationContext(), "Se ha subido la foto al storage correctamente", Toast.LENGTH_SHORT).show();
                    });

                    new File(currentPhotoPath).delete(); //Borra la foto almacenada tras subirla al storage
                } else
                    Toast.makeText(getApplicationContext(), "No se ha podido subir la foto al storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadProgressMessage() {
        progressMsg.setTitle("Subiendo imagen a Firebase Storage");
        progressMsg.setMessage("Espere un momento, por favor...");
        progressMsg.setCancelable(false);
        progressMsg.show();
    }

    private File createImageFile() throws IOException {
        /*/ Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_"; */

        return File.createTempFile(
                "camera_test",  /* prefix */
                ".jpg",         /* suffix */
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)      /* directory */
        );
    }

    private byte[] convertImageToByteArray(String PhotoPath) {
        Bitmap bitmapOrig = BitmapFactory.decodeFile(PhotoPath); //crear bitmap desde ubicación tipo string
        //bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriPhoto); //crear bitmap desde uri

        int bmpWidth = bitmapOrig.getWidth();
        int bmpHeight = bitmapOrig.getHeight();
        Matrix mat = new Matrix();

        float scale = Math.min((float) downloadedImgView.getWidth()/bmpWidth, (float) downloadedImgView.getHeight()/bmpHeight);
        mat.postScale(scale, scale);
        mat.postRotate(90);

        Bitmap bitmap = Bitmap.createBitmap(bitmapOrig, 0,0, bmpWidth, bmpHeight, mat, true);
        bitmapOrig.recycle();

        //Convierte el bitmap en un array de bytes
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 80, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        return byteArray;
    }
}