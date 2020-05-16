package com.aueb.urbanarts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditAccountActivity extends AppCompatActivity {

    final String TAG = "123";
    private final int PICK_IMAGE_REQUEST = 22;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    private Bitmap bitmap;
    private String photoPath;
    private boolean changePhoto = false;
    Spinner sItems;
    private Uri filePath;
    String artistName;
    String artistGenre;
    String artistDescription;
    final List<String> artist_type = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DocumentReference docArtist = db.collection("users").document(user.getUid());
        docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    final DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
// ARTIST
                        if (document.getBoolean("is_artist")) {
                            setContentView(R.layout.edit_artist_account);
                            final ConstraintLayout dialog = findViewById(R.id.dialog);
                            final RelativeLayout wholeLayout = findViewById(R.id.constraint);
                            dialog.setVisibility(View.INVISIBLE);
                            wholeLayout.setBackgroundColor(Color.WHITE);

                            final ProgressBar imageProg = findViewById(R.id.image_progress);
                            final CircleImageView profileImage = findViewById(R.id.artist_image);
                            imageProg.setVisibility(View.VISIBLE);

                            getArtistInformation(user.getUid());
                            changeGenre(artist_type);
                            showProfileImage(profileImage, imageProg);


                            findViewById(R.id.artist_image).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showFileChooser();
                                }
                            });

                            findViewById(R.id.deactivate).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    deactivateArtistAccount();
                                }
                            });

                            findViewById(R.id.terminate).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    terminateAccount(document);
                                }
                            });

                            findViewById(R.id.update).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if (sItems != null) {
                                        EditText oldPassword = findViewById(R.id.old_password);
                                        final EditText newPassword = findViewById(R.id.new_password);

                                        if (!oldPassword.getText().toString().equals("") && !newPassword.getText().toString().equals("")) {
                                            updateEverything(oldPassword, newPassword);
                                        } else {
                                            updateInfo();
                                        }
                                    }
                                }
                            });

                            findViewById(R.id.view_artist_profile).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {

                                    db.collection("artists")
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {

                                                            if (document.getString("user_id").equals(user.getUid())) {

                                                                goArtistProfile();

                                                            }

                                                        }
                                                    } else {
                                                        Log.w("123", "Error getting documents.", task.getException());
                                                    }
                                                }
                                            });
                                }
                            });

// USER
                        } else {
                            setContentView(R.layout.edit_user_account);

                            findViewById(R.id.request_artist).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    goArtistAccountRequest();
                                }
                            });

                            findViewById(R.id.terminate).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    terminateAccount(document);
                                }
                            });

                            findViewById(R.id.update).setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {

                                    Map<String, Object> userMap = new HashMap<>();
                                    EditText username = findViewById(R.id.username);
                                    EditText oldPassword = findViewById(R.id.old_password);
                                    final EditText newPassword = findViewById(R.id.new_password);
                                    final boolean[] changedSomething = {false};

                                    if (!username.getText().toString().equals("")) {
                                        userMap.put("username", username.getText().toString());
                                        changedSomething[0] = true;
                                    }
                                    if (!oldPassword.getText().toString().equals("") && !newPassword.getText().toString().equals("")) {

                                        AuthCredential credential = EmailAuthProvider
                                                .getCredential(user.getEmail(), oldPassword.getText().toString());

                                        user.reauthenticate(credential)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            user.updatePassword(newPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Log.d("123", "Password updated");
                                                                        changedSomething[0] = true;
                                                                    } else {
                                                                        Log.d("123", "Error password not updated");
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            Log.d("123", "Error auth failed");
                                                            Toast.makeText(getApplicationContext(), "Wrong Password!", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                    }

                                    if (changedSomething[0]) {
                                        db.collection("users").document(user.getUid()).update(userMap);
                                        Toast.makeText(getApplicationContext(), "Update Successful!", Toast.LENGTH_LONG).show();
                                        goHomePage();
                                    }
                                }
                            });
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void updateEverything(EditText oldPassword, final EditText newPassword) {
        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), oldPassword.getText().toString());

        final ConstraintLayout dialogBox = findViewById(R.id.dialog);
        final RelativeLayout wholeLayout = findViewById(R.id.constraint);
        final TextView percentage = findViewById(R.id.perc);
        dialogBox.setVisibility(View.VISIBLE);
        wholeLayout.setBackgroundColor(Color.parseColor("#808080"));
        percentage.setText("Uploading...");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("123", "Password updated");

                                        updateInfo();

                                    } else {
                                        Log.d("123", "Error password not updated");
                                    }

                                    dialogBox.setVisibility(View.INVISIBLE);
                                    wholeLayout.setBackgroundColor(Color.WHITE);
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                }
                            });
                        } else {
                            dialogBox.setVisibility(View.INVISIBLE);
                            wholeLayout.setBackgroundColor(Color.WHITE);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            Toast.makeText(getApplicationContext(), "Wrong Password!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateInfo() {
        Map<String, Object> userMap = new HashMap<>();
        final Map<String, Object> artistMap = new HashMap<>();
        EditText username = findViewById(R.id.username);
        EditText description = findViewById(R.id.description);
        String artistType = sItems.getSelectedItem().toString();
        final boolean[] changedSomething = {false};

        if (!username.getText().toString().equals("")) {
            userMap.put("username", username.getText().toString());
            artistMap.put("display_name", username.getText().toString());
            changedSomething[0] = true;
        }

        artistMap.put("genre", artistType);
        changedSomething[0] = true;

        if (!description.getText().toString().equals("")) {
            artistMap.put("description", description.getText().toString());
            changedSomething[0] = true;
        }

        if (changedSomething[0] || changePhoto) {
            db.collection("users").document(user.getUid()).update(userMap);

            DocumentReference docArtist = db.collection("artists").document(user.getUid());
            docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            db.collection("artists").document(document.getId()).update(artistMap);
                        }
                    } else {
                        Log.w("123", "Error getting documents.", task.getException());
                    }
                }
            });

            if (changePhoto) {
                uploadPhoto();
            } else {
                Toast.makeText(getApplicationContext(), "Update Successful!", Toast.LENGTH_LONG).show();
                goHomePage();
            }

        }
    }

    private void getArtistInformation(String artist_id) {
        final EditText artistNameDisplay = findViewById(R.id.username);
        final EditText descriptionDisplay = findViewById(R.id.description);

        DocumentReference docArtist = db.collection("artists").document(artist_id);
        docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                        artistName = document.getString("display_name");
                        artistGenre = document.getString("genre");
                        artistDescription = document.getString("description");

                        artistNameDisplay.setHint(artistName);
                        descriptionDisplay.setHint(artistDescription);
                        artist_type.add(artistGenre);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void terminateAccount(final DocumentSnapshot document) {

        final String[] m_Text = {""};

        AlertDialog.Builder builder = new AlertDialog.Builder(EditAccountActivity.this);
        builder.setTitle("WARNING!");
        builder.setMessage("\nThis action will actually TERMINATE your account! This is PERMANENT. Are you sure? \n\n\nConfirm with Password: \n");
        final EditText input = new EditText(EditAccountActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text[0] = input.getText().toString();

                if (!m_Text[0].isEmpty()) {
                    final AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), m_Text[0]);
                    final TextView percentage = findViewById(R.id.perc);

                    final ConstraintLayout dialogBox = findViewById(R.id.dialog);
                    final RelativeLayout wholeLayout = findViewById(R.id.constraint);
                    percentage.setText("Just a moment...");
                    dialogBox.setVisibility(View.VISIBLE);
                    wholeLayout.setBackgroundColor(Color.parseColor("#808080"));

                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    // Prompt the user to re-provide their sign-in credentials
                    user.reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        if (document.getBoolean("is_artist")) {
                                            deactivateArtistAccountFirst(credential);
                                            deleteUser();
                                        } else {
                                            deleteUser();
                                        }
                                    } else {
                                        dialogBox.setVisibility(View.INVISIBLE);
                                        wholeLayout.setBackgroundColor(Color.WHITE);
                                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                        Toast.makeText(getApplicationContext(), "Wrong Password!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    private void deactivateArtistAccountFirst(AuthCredential credential) {
        final Map<String, Object> userMap = new HashMap<>();
        userMap.put("is_artist", false);

        DocumentReference docArtist = db.collection("artists").document(user.getUid());
        docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if (document.getString("profile_image_url").equals("none")) {
                            deleteArtist_NOTImage(userMap);
                        } else {
                            deleteArtist_ANDImage(userMap, document);
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    private void deleteUser() {
        final TextView percentage = findViewById(R.id.perc);

        final ConstraintLayout dialogBox = findViewById(R.id.dialog);
        final RelativeLayout wholeLayout = findViewById(R.id.constraint);
        percentage.setText("Just a moment...");
        dialogBox.setVisibility(View.VISIBLE);
        wholeLayout.setBackgroundColor(Color.parseColor("#808080"));

        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            db.collection("users").document(user.getUid())
                                    .delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getApplicationContext(), "Account Deleted!", Toast.LENGTH_LONG).show();
                                                goHomePage();
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Couldn't Delete Document", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(getApplicationContext(), "Couldn't Delete User", Toast.LENGTH_LONG).show();
                        }
                        dialogBox.setVisibility(View.INVISIBLE);
                        wholeLayout.setBackgroundColor(Color.WHITE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
    }

    private void deactivateArtistAccount() {

        final Map<String, Object> userMap = new HashMap<>();
        userMap.put("is_artist", false);
        final String[] m_Text = {""};

        AlertDialog.Builder builder = new AlertDialog.Builder(EditAccountActivity.this);
        builder.setTitle("WARNING!");
        builder.setMessage("\nThis action will actually DEACTIVATE your account! Your Artist Account will be deleted. This is PERMANENT. Are you sure? \n\n\nConfirm with Password: \n");
        final EditText input = new EditText(EditAccountActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                m_Text[0] = input.getText().toString();

                if (!m_Text[0].isEmpty()) {
                    final TextView percentage = findViewById(R.id.perc);
                    final ConstraintLayout dialogBox = findViewById(R.id.dialog);
                    final RelativeLayout wholeLayout = findViewById(R.id.constraint);
                    percentage.setText("Just a moment...");
                    dialogBox.setVisibility(View.VISIBLE);
                    wholeLayout.setBackgroundColor(Color.parseColor("#808080"));

                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), m_Text[0]);

                    // Prompt the user to re-provide their sign-in credentials
                    user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                DocumentReference docArtist = db.collection("artists").document(user.getUid());
                                docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                if (document.getString("profile_image_url").equals("none")) {
                                                    deleteArtist_NOTImage(userMap);
                                                } else {
                                                    deleteArtist_ANDImage(userMap, document);
                                                }
                                            } else {
                                                Log.d(TAG, "No such document");
                                            }
                                        } else {
                                            dialogBox.setVisibility(View.INVISIBLE);
                                            wholeLayout.setBackgroundColor(Color.WHITE);
                                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                            Log.d(TAG, "get failed with ", task.getException());
                                        }
                                    }
                                });
                            } else {
                                dialogBox.setVisibility(View.INVISIBLE);
                                wholeLayout.setBackgroundColor(Color.WHITE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(getApplicationContext(), "Wrong Password!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void deleteArtist_ANDImage(final Map<String, Object> userMap, DocumentSnapshot document) {
        StorageReference desertRef = mFirebaseStorage.getReferenceFromUrl(document.getString("profile_image_url"));

        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                db.collection("artists").document(user.getUid())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                db.collection("users").document(user.getUid()).update(userMap);
                                Log.d("123", "Account Deactivated Successfully!");
                                Toast.makeText(getApplicationContext(), "Account Deactivated Successfully!", Toast.LENGTH_LONG).show();
                                goHomePage();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("123", "Error!!", e);
                                Toast.makeText(getApplicationContext(), "Error!!", Toast.LENGTH_LONG).show();
                                goHomePage();
                            }
                        });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });
    }

    private void deleteArtist_NOTImage(final Map<String, Object> userMap) {
        db.collection("artists").document(user.getUid())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("users").document(user.getUid()).update(userMap);
                        Log.d("123", "Account Deactivated Successfully!");
                        Toast.makeText(getApplicationContext(), "Account Deactivated Successfully!", Toast.LENGTH_LONG).show();
                        goHomePage();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("123", "Error!!", e);
                        Toast.makeText(getApplicationContext(), "Error!!", Toast.LENGTH_LONG).show();
                        goHomePage();
                    }
                });
    }

    private void showProfileImage(final CircleImageView profileImage, final ProgressBar imageProg) {
        final String[] imageURL = new String[1];
        db.collection("artists")
                .whereEqualTo("user_id", user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                imageURL[0] = String.valueOf(document.get("profile_image_url"));
                                if (!imageURL[0].equals("none")) {
                                    Picasso.with(getApplicationContext()).load(imageURL[0]).into(profileImage, new com.squareup.picasso.Callback() {
                                        @Override
                                        public void onSuccess() {
                                            imageProg.setVisibility(View.INVISIBLE);
                                        }

                                        @Override
                                        public void onError() {

                                        }
                                    });
                                } else {
                                    profileImage.setImageResource(R.drawable.profile);
                                    imageProg.setVisibility(View.INVISIBLE);
                                }
                            }

                        }
                    }
                });
    }

    private void changeGenre(final List<String> artist_type) {
        fStore.collection("genre")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (!document.getId().equals(artistGenre))
                                    artist_type.add(document.getId());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(EditAccountActivity.this, android.R.layout.simple_spinner_item, artist_type);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            sItems = (Spinner) findViewById(R.id.genre);
                            sItems.setAdapter(adapter);
                        } else {
                            Log.d("123", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    public void uploadPhoto() {

        if (filePath != null) {
            final StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            final TextView percentage = findViewById(R.id.perc);
            final ConstraintLayout dialogBox = findViewById(R.id.dialog);
            final RelativeLayout wholeLayout = findViewById(R.id.constraint);
            dialogBox.setVisibility(View.VISIBLE);
            wholeLayout.setBackgroundColor(Color.parseColor("#808080"));
            percentage.setText("Uploading...");

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            final StorageReference riversRef = storageReference.child("profiles/profile" + user.getUid() + new Date().getTime() + ".jpg");


            riversRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Toast.makeText(getApplicationContext(), "New Image Uploaded!", Toast.LENGTH_LONG).show();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            dialogBox.setVisibility(View.INVISIBLE);
                            wholeLayout.setBackgroundColor(Color.WHITE);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            double prog = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            percentage.setText("Uploading... " + ((int) prog) + "%");
                        }
                    });

            riversRef.putFile(filePath).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {

                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return riversRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        if (downloadUri == null)
                            return;
                        else {

                            percentage.setText("Just a moment...");
                            dialogBox.setVisibility(View.VISIBLE);
                            wholeLayout.setBackgroundColor(Color.parseColor("#808080"));
                            photoPath = String.valueOf(downloadUri);

                            DocumentReference docArtist = db.collection("artists").document(user.getUid());
                            docArtist.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        final DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {

                                            if (!document.getString("profile_image_url").equals("none")) {
                                                StorageReference desertRef = mFirebaseStorage.getReferenceFromUrl(document.getString("profile_image_url"));
                                                desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Map<String, Object> artistMap = new HashMap<>();
                                                        artistMap.put("profile_image_url", photoPath);
                                                        db.collection("artists").document(document.getId()).update(artistMap);
                                                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                                        Toast.makeText(getApplicationContext(), "Account Updated!", Toast.LENGTH_LONG).show();
                                                        goHomePage();
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception exception) {
                                                        dialogBox.setVisibility(View.INVISIBLE);
                                                        wholeLayout.setBackgroundColor(Color.WHITE);
                                                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                                    }
                                                });

                                            } else {
                                                Map<String, Object> artistMap = new HashMap<>();
                                                artistMap.put("profile_image_url", photoPath);
                                                db.collection("artists").document(document.getId()).update(artistMap);
                                            }
                                        } else {
                                            Log.d(TAG, "No such document");
                                        }
                                    } else {
                                        Log.d(TAG, "get failed with ", task.getException());
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                CircleImageView image = findViewById(R.id.artist_image);
                image.setImageBitmap(bitmap);
                changePhoto = true;
            } catch (IOException e) {

            }
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    public void goHomePage() {
        Intent myIntent = new Intent(EditAccountActivity.this, HomePage.class);
        startActivity(myIntent);
        Animatoo.animateZoom(this);
        finish();
    }

    private void goArtistProfile() {
        Intent intent = new Intent(EditAccountActivity.this, ArtistProfileActivity.class);
        intent.putExtra("ARTIST_DOCUMENT_ID", user.getUid());
        startActivity(intent);
        Animatoo.animateFade(this);
        finish();
    }

    private void goArtistAccountRequest() {
        Intent intent = new Intent(EditAccountActivity.this, ArtistAccountRequestActivity.class);
        startActivity(intent);
        Animatoo.animateCard(this);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goHomePage();
    }
}
