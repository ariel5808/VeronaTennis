
package com.example.veronatennis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private EditText etEmail, etPassword, etConfirm;
    private EditText etFirstName, etLastName, etAddress;

    private Button btnRegister;
    private ProgressBar progress;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etRegEmail);
        etPassword = view.findViewById(R.id.etRegPassword);
        etConfirm = view.findViewById(R.id.etRegConfirm);

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etAddress = view.findViewById(R.id.etAddress);

        btnRegister = view.findViewById(R.id.btnRegister);
        progress = view.findViewById(R.id.progressRegister);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {

        String email = etEmail.getText()
                .toString().trim();

        String password = etPassword.getText()
                .toString();

        String confirm = etConfirm.getText()
                .toString();

        String firstName = etFirstName.getText()
                .toString().trim();

        String lastName = etLastName.getText()
                .toString().trim();

        String address = etAddress.getText()
                .toString().trim();

        // Validate input
        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Invalid email");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(
                    "Password must contain at least 6 characters");
            return;
        }

        if (!password.equals(confirm)) {
            etConfirm.setError("Passwords do not match");
            return;
        }

        if (firstName.isEmpty()) {
            etFirstName.setError("Required");
            return;
        }

        if (lastName.isEmpty()) {
            etLastName.setError("Required");
            return;
        }

        if (address.isEmpty()) {
            etAddress.setError("Required");
            return;
        }

        setLoading(true);

        FirebaseUser currentUser = auth.getCurrentUser();

        // Retry profile saving if Auth was already created,
        // but the Firestore write previously failed.
        if (currentUser != null
                && email.equalsIgnoreCase(currentUser.getEmail())) {

            saveUser(currentUser.getUid(),
                    email, firstName, lastName, address);
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {

                    if (!isAdded()) return;

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            saveUser(user.getUid(),
                                    email, firstName,
                                    lastName, address);
                        } else {
                            setLoading(false);
                            showError("Could not obtain user ID");
                        }

                    } else {
                        setLoading(false);

                        Exception e = task.getException();

                        showError(e != null
                                ? e.getMessage()
                                : "Registration failed");
                    }
                });
    }

    private void saveUser(
            String uid,
            String email,
            String firstName,
            String lastName,
            String address) {

        Map<String, Object> userData = new HashMap<>();

        userData.put("email", email);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("address", address);

        db.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    if (!isAdded()) return;

                    setLoading(false);

                    Toast.makeText(
                            requireContext(),
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    ((AuthActivity) requireActivity())
                            .openNextActivity();
                })
                .addOnFailureListener(e -> {

                    if (!isAdded()) return;

                    setLoading(false);

                    showError("Profile save failed: "
                            + e.getMessage());
                });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progress.setVisibility(
                loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(),
                message, Toast.LENGTH_LONG).show();
    }
}
