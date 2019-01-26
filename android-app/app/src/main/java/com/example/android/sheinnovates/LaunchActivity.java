package com.example.android.sheinnovates;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LaunchActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_MULTIPLE = 1001;
    private Uri imageUri;
    private static final String TAG = "LaunchActivity";

    //main data
    public static ArrayList<JSONObject> imagesdata = new ArrayList<JSONObject>();
    public static JSONObject imagesjsonobj = new JSONObject();
    // references to our images
    public static Integer[] mThumbIds = {
            R.drawable.test, R.drawable.test1,
            R.drawable.test2, R.drawable.test3,
            R.drawable.test4, R.drawable.test5,
            R.drawable.test, R.drawable.test1,
            R.drawable.test2, R.drawable.test3,
            R.drawable.test4, R.drawable.test5,
            R.drawable.test, R.drawable.test1,
            R.drawable.test2, R.drawable.test3,
            R.drawable.test4, R.drawable.test5,
            R.drawable.test, R.drawable.test1,
            R.drawable.test2, R.drawable.test3,
            R.drawable.test4, R.drawable.test5
    };
    public static ArrayList<Uri> mThumbUris = new ArrayList<Uri>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                Intent myIntent = new Intent(LaunchActivity.this, MainActivity.class);
//                myIntent.putExtra("key", value); //Optional parameters
                LaunchActivity.this.startActivity(myIntent);
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click upload image
                startChooseImageIntentForResult();

            }
        });

    }

    private void startChooseImageIntentForResult() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            List<String> imagesEncodedList = new ArrayList<String>();
            if(data.getData()!=null) {
                imageUri = data.getData();
                // Get the cursor
                Cursor cursor = getContentResolver().query(imageUri,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imageEncoded = cursor.getString(columnIndex);
                cursor.close();

                mThumbUris.add(imageUri);
                try{
                    imagesjsonobj.put(imageUri.toString(), new JSONObject());
                }catch (JSONException e){
                    //failed
                }
                Log.e("MYLIFESWORK",imageUri.toString());
                //get bitmap for processing
                try{
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    getLabels(imageBitmap, imageUri.toString());
                    getFaceDetection(imageBitmap, imageUri.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Error getting bitmap image");
                }
                //tryReloadAndDetectInImage();
            } else {
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {

                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        mThumbUris.add(uri);
                        try{
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            getLabels(imageBitmap,uri.toString());
                            getFaceDetection(imageBitmap, uri.toString());
                        } catch (IOException e) {
                            Log.e(TAG, "Error getting bitmap image");
                        }
                        // Get the cursor
                        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                        // Move to first row
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String imageEncoded  = cursor.getString(columnIndex);
                        imagesEncodedList.add(imageEncoded);
                        cursor.close();
                    }
                    Log.v("LOG_TAG", "Selected Images" + mThumbUris.size());
                }
            }
            gotoGallery();
        }
    }

    private void gotoGallery() {

        Intent myIntent = new Intent(LaunchActivity.this, MainActivity.class);
        //myIntent.putExtra("key", value); //Optional parameters
        LaunchActivity.this.startActivity(myIntent);
    }

    private void getLabels(Bitmap bitmap, final String uristring) {
        FirebaseApp.initializeApp(this);
        final JSONObject imgjsonobj = new JSONObject();
        try {
            imgjsonobj.put("uri",uristring);
        } catch (JSONException e) {
            //failed
        }

        Log.e("MYLIFESWORK","here");
        FirebaseVisionCloudDetectorOptions options =
                new FirebaseVisionCloudDetectorOptions.Builder()
                        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                        .setMaxResults(15)
                        .build();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionCloudLabelDetector detector = FirebaseVision.getInstance()
                .getVisionCloudLabelDetector(options);
        Task<List<FirebaseVisionCloudLabel>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
                                        // Task completed successfully
                                        JSONObject labeljsonobj = new JSONObject();
                                        for (FirebaseVisionCloudLabel label: labels) {
                                            String text = label.getLabel();
                                            String entityid = label.getEntityId();
                                            float confidence = label.getConfidence();
                                            try{
                                                labeljsonobj.put(label.getLabel(),confidence);
                                            } catch (JSONException e) {
                                                //failed
                                            }
                                            Log.e("MYLIFESWORK", text+entityid+confidence);
                                        }
                                        try {
                                            (imagesjsonobj.getJSONObject(uristring)).put("labels",labeljsonobj);
                                            Log.e("RIP",imagesjsonobj.toString());
                                            imgjsonobj.put("labels", labeljsonobj);
                                            imagesdata.add(imgjsonobj);
                                            Log.e("okokok",imgjsonobj.toString());
                                        } catch (JSONException e) {
                                            //failed
                                            Log.e("bigsad","rip me");
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

    }

    private void getFaceDetection(Bitmap bitmap, final String uristring) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully
                                        float smileProb = 0;
                                        float eyeOpenProb = 0;
                                        for (FirebaseVisionFace face : faces) {
                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                smileProb = smileProb + face.getSmilingProbability();
                                            }
                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                eyeOpenProb = eyeOpenProb + face.getRightEyeOpenProbability();
                                            }
                                            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                eyeOpenProb = eyeOpenProb + face.getLeftEyeOpenProbability();
                                            }

                                        }
                                        float qualityscore = 0;
                                        Log.e("faces size: ",Integer.toString(faces.size()));
                                        if (faces.size() != 0) {
                                            smileProb = smileProb / faces.size();
                                            eyeOpenProb = eyeOpenProb / (2 * faces.size());
                                            qualityscore = 2*smileProb + eyeOpenProb;
                                        }
                                        Log.e("SCORE",Float.toString(qualityscore));

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

    }

}
