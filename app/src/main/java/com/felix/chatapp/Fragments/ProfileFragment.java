package com.felix.chatapp.Fragments;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.felix.chatapp.Models.User;
import com.felix.chatapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private CircleImageView imageProfile;
    private MaterialEditText inputName, inputStatus, inputBio;
    private Button btnUpdateProfile;

    DatabaseReference userReference;
    FirebaseUser fUser;

//  Firebase Storage implementation
    StorageReference storageReference, previousImageLocation;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        fUser = FirebaseAuth.getInstance().getCurrentUser();

        imageProfile = view.findViewById(R.id.profileImage);
        inputName = view.findViewById(R.id.profileName);
        inputStatus = view.findViewById(R.id.inputStatus);
        inputBio = view.findViewById(R.id.inputBio);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);

//      Set button update to visible
        ArrayList<MaterialEditText> inputs = new ArrayList<>();
        inputs.add(inputName);
        inputs.add(inputStatus);
        inputs.add(inputBio);

        for (MaterialEditText editText : inputs) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    btnUpdateProfile.setVisibility(View.VISIBLE);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

//      Load & update profile
        storageReference = FirebaseStorage.getInstance().getReference("Uploads/profileImage");
        userReference = FirebaseDatabase.getInstance(requireContext().getString(R.string.databaseURL))
                        .getReference("Users")
                        .child(fUser.getUid());
        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) return;

                inputName.setText(user.getName());
                inputBio.setText(user.getBio());
                inputStatus.setText(user.getStatus());
                btnUpdateProfile.setVisibility(View.GONE);

                if (user.getImageURL().equals("default")) {
                    imageProfile.setImageResource(R.drawable.nophoto);
                } else {
                    if (isAdded() && getActivity() != null) {
                        Glide.with(requireContext()).load(user.getImageURL()).into(imageProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ProfileFragment", error.getMessage());
            }
        });

        btnUpdateProfile.setOnClickListener(view1 -> {
            String name = inputName.getText().toString();
            String status = inputStatus.getText().toString();
            String bio = inputBio.getText().toString();

            HashMap<String, Object> profile = new HashMap<>();
            profile.put("name", name);
            profile.put("status", status);
            profile.put("bio", bio);
            profile.put("search", name.toLowerCase(Locale.ROOT));
            userReference.updateChildren(profile);
        });

        imageProfile.setOnClickListener(view1 -> openImage());

        return view;
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog pd = new ProgressDialog(requireContext());
        pd.setMessage("Uploading");
        pd.show();

        if (imageUri != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return fileReference.getDownloadUrl();
//                  Upload then return Task<Uri>
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
//                      Get uri of the uploaded image in Firebase
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

//                      Delete previous image
                        deleteFile(userReference, "imageURL");

//                      Save the new uri in user record
                        HashMap<String, Object> profileImageUrl = new HashMap<>();
                        profileImageUrl.put("imageURL", mUri);
                        userReference.updateChildren(profileImageUrl);

                        pd.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()) {
                Toast.makeText(requireContext(), "Upload in progress", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

    public void deleteFile(DatabaseReference reference, String key) {
        DatabaseReference previousImage = reference.child(key);
        previousImage.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.getValue(String.class);
                if (!imageUrl.equals("default")){
                    previousImageLocation = FirebaseStorage.getInstance().getReferenceFromUrl(snapshot.getValue(String.class));
                    previousImageLocation.delete();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Delete error : ", error.getMessage());
            }
        });
    }
}