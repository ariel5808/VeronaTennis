
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

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;

    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        progressLogin = view.findViewById(R.id.progressLogin);

        auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {

        String email = etEmail.getText()
                .toString().trim();

        String password = etPassword.getText()
                .toString();

        if (email.isEmpty()) {
            etEmail.setError("Enter email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Enter password");
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {

                    if (!isAdded()) return;

                    setLoading(false);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                requireContext(),
                                "Login successful!",
                                Toast.LENGTH_SHORT).show();

                        ((AuthActivity) requireActivity())
                                .openNextActivity();

                    } else {

                        Toast.makeText(
                                requireContext(),
                                "Login failed: " +
                                        getErrorMessage(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressLogin.setVisibility(
                loading ? View.VISIBLE : View.GONE);
    }

    private String getErrorMessage(Exception e) {
        return e == null ? "Unknown error" : e.getMessage();
    }
}
