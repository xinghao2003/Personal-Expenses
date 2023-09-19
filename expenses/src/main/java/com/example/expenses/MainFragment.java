package com.example.expenses;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.CursorWindow;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MainFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            getArguments().getString(ARG_PARAM1);
            getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initialize();
    }

    protected AppDatabase db = null;
    protected Bitmap imageBitmap = null;

    protected SharedPreferences preferences = null;
    protected SharedPreferences.Editor editor = null;

    protected TextView tvMonthlyLimit, tvCurrentMonthlySpending, tvRemainingDays, tvCurrentDailyLimit, tvLastRecord;

    protected TextInputEditText etAmount, etPurpose;

    protected ChipGroup categoryGroup;

    protected ImageView ivEvidence;

    protected TextView tvEvidence;

    protected ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    protected TextView tvLocation;

    protected Button bReset, bSave;

    private FusedLocationProviderClient fusedLocationClient;

    private final AtomicReference<Double> lat = new AtomicReference<>((double) 0);
    private final AtomicReference<Double> lon = new AtomicReference<>((double) 0);

    private void initialize(){
        db = ((MainActivity) requireActivity()).accessDb();

        components();

        firstTime();

        newMonth();

        update();

        new Thread(() -> {
            //Some kind of hack???
            try {
                Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
                field.setAccessible(true);
                field.set(null, 100 * 1024 * 1024); //the 100MB is the new size
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                ExpensesDao expensesDao = db.expensesDao();
                List<Expense> expenses = expensesDao.getAll();

                for (Expense expense : expenses) {
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(expense.timeStamp));
                    Boolean image = expense.evidence != null;
                    Log.i("ExpensesDao", String.format("Time : %s ; Amount : %s ; Category : %s ; Purpose : %s ; Image : %s ; Loc : %s , %s",
                            formattedDate, expense.amount, expense.category, expense.purpose, image, expense.latitude, expense.longitude));
                }
            } catch (NullPointerException npe) {
                Log.w("NPE", npe.toString());
                Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
            }
        }).start();
    }

    private void firstTime() {
        // Get the values for the keys
        float monthlyLimit = preferences.getFloat("monthlyLimit", 0.00f);

        if (monthlyLimit == 0.00f) {
            editMonthlyLimit();
        }
    }

    private void editMonthlyLimit() {
        // Declare a new AlertDialog.Builder object
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Set the dialog title
        builder.setTitle(R.string.mlDialog);

        // Set the input field
        final EditText input = new EditText(requireContext());
        builder.setView(input);

        // Set the positive button
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Get the value from the input field
            String value = input.getText().toString();

            // Do something with the value
            Log.i("Edit Monthly Limit", value);

            float monthlyLimit;
            try {
                monthlyLimit = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                monthlyLimit = 0.00f;
            }
            editor.putFloat("monthlyLimit", monthlyLimit);
            editor.apply();

            update();
        });

        // Set the negative button

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void components() {
        // Get the SharedPreferences object
        preferences = requireActivity().getSharedPreferences("localData", MODE_PRIVATE);
        editor = preferences.edit();

        tvMonthlyLimit = requireView().findViewById(R.id.tvMonthlyLimit);
        tvMonthlyLimit.setOnClickListener(v -> editMonthlyLimit());

        tvCurrentMonthlySpending = requireView().findViewById(R.id.tvCurrentMonthlySpending);
        tvRemainingDays = requireView().findViewById(R.id.tvRemainingDays);
        tvCurrentDailyLimit = requireView().findViewById(R.id.tvCurrentDailyLimit);
        tvLastRecord = requireView().findViewById(R.id.tvLastRecord);

        etAmount = requireView().findViewById(R.id.etAmount);
        etPurpose = requireView().findViewById(R.id.etPurpose);

        categoryGroup = requireView().findViewById(R.id.cgPurpose);

        tvEvidence = requireView().findViewById(R.id.tvEvidence);

        ivEvidence = requireView().findViewById(R.id.ivEvidence);
        ivEvidence.setOnClickListener(v -> showImagePickerDialog());

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: " + uri);

                ivEvidence.setImageURI(uri);

                imageBitmap = ((BitmapDrawable) ivEvidence.getDrawable()).getBitmap();

                ivEvidence.setMaxHeight(imageBitmap.getHeight());

                tvEvidence.setVisibility(View.INVISIBLE);

                etAmount.clearFocus();
                etPurpose.clearFocus();

            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        tvLocation = requireView().findViewById(R.id.tvLocation);
        tvLocation.setOnClickListener(v -> updateLocation());

        bReset = requireView().findViewById(R.id.bReset);
        bReset.setOnClickListener(v -> reset());

        bSave = requireView().findViewById(R.id.bSave);
        bSave.setOnClickListener(v -> save());
    }

    private void newMonth() {
        long previousTimeMillis = preferences.getLong("lastRecord", System.currentTimeMillis());

        // Create a new calendar instance and set the time to the previous time
        Calendar previousCalendar = Calendar.getInstance();
        previousCalendar.setTimeInMillis(previousTimeMillis);

        // Get the month and year of the previous time
        int previousMonth = previousCalendar.get(Calendar.MONTH);
        int previousYear = previousCalendar.get(Calendar.YEAR);

        // Get the current calendar instance
        Calendar currentCalendar = Calendar.getInstance();

        // Get the current month and year
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);

        // Check if the current month and year are the same as the previous month and year
        if (currentMonth == previousMonth && currentYear == previousYear) {
            // The current month is the same as the previous month
            Log.i("Month Check", "The current month is the same as the previous month.");
        } else {
            // The current month is a new month
            Expense newExpense = new Expense(System.currentTimeMillis(), preferences.getFloat("currentMonthlySpending", 0.00f), "Summary of the month", "End of month", null, 0, 0);

            new Thread(() -> {
                try {
                    ExpensesDao expensesDao = db.expensesDao();
                    expensesDao.insertAll(newExpense);
                } catch (NullPointerException npe) {
                    Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
                    Log.w("NPE", npe.toString());
                }
            }).start();

            editor.putFloat("currentMonthlySpending", 0.00f);
            editor.apply();

            Toast.makeText(requireContext(), "New month", Toast.LENGTH_SHORT).show();
            Log.i("Month Check", "The current month is a new month.");
        }
    }

    private void update() {
        // Get the values for the keys
        float monthlyLimit = preferences.getFloat("monthlyLimit", 0.00f);
        tvMonthlyLimit.setText(getString(R.string.mLimit, monthlyLimit));

        float currentMonthlySpending = preferences.getFloat("currentMonthlySpending", 0.00f);
        tvCurrentMonthlySpending.setText(getString(R.string.cmSpending, currentMonthlySpending));

        LocalDate currentDate = LocalDate.now();
        // get the current year and month
        int currentYear = currentDate.getYear();
        int currentMonth = currentDate.getMonthValue();
        // get the number of days in the current month
        int daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth();
        // calculate the number of remaining days in the current month, include the current date
        int remainingDays = daysInMonth - currentDate.getDayOfMonth() + 1;

        tvRemainingDays.setText(getString(R.string.rDays, remainingDays));

        float currentDailyLimit = (monthlyLimit - currentMonthlySpending) / remainingDays;
        tvCurrentDailyLimit.setText(getString(R.string.cdLimit, currentDailyLimit));

        long lastRecord = preferences.getLong("lastRecord", 0);
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(lastRecord));

        new Thread(() -> {
            try {
                ExpensesDao expensesDao = db.expensesDao();
                List<Expense> expenses = expensesDao.loadAllByTimeStamp(preferences.getLong("lastRecord", 0));

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Expense expense : expenses) {
                            tvLastRecord.setText(getString(R.string.lRecord, expense.amount, expense.category, expense.purpose, formattedDate));
                            tvLastRecord.setOnClickListener(v -> openMaps(expense.latitude, expense.longitude));
                        }
                    }
                });

            } catch (NullPointerException npe) {
                Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
                Log.w("NPE", npe.toString());
            }
        }).start();

        updateLocation();
    }

    private void reset() {
        etAmount.setText("");
        etPurpose.setText("");
        ivEvidence.setImageBitmap(null);
        ivEvidence.setMaxHeight(0);
        tvEvidence.setVisibility(View.VISIBLE);
        categoryGroup.check(R.id.chipOther);
    }

    private boolean check() {
        if (Objects.requireNonNull(etAmount.getText()).toString().equals("") || Float.parseFloat(etAmount.getText().toString()) <= 0.00) {
            Toast.makeText(requireContext(), "Invalid Amount", Toast.LENGTH_SHORT).show();
            etAmount.requestFocus();

            return false;
        } else {
            return true;
        }
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        }).addOnSuccessListener(location -> {
            // Got last known location. In some rare situations this can be null.
            if (location == null) {
                Log.d("Curr Location", "Cannot get location.");
                Toast.makeText(requireContext(), "Cannot get location.", Toast.LENGTH_SHORT).show();
            } else {
                lat.set(location.getLatitude());
                lon.set(location.getLongitude());
                tvLocation.setText(getString(R.string.locationText, lat.get(), lon.get()));

                Log.d("Current Location", String.format("%s, %s", lat.get(), lon.get()));
                Toast.makeText(requireContext(), "Updated to current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openMaps(double latitude, double longitude){
        // Create a Uri from an intent string. Use the result to create an Intent.
        Log.d("Open Maps", String.format(Locale.getDefault(),"geo:%.6f,%.6f?z=16?q=%.6f,%.6f", latitude, longitude, latitude, longitude));
        Uri gmmIntentUri = Uri.parse(String.format(Locale.getDefault(),"geo:%.6f,%.6f?z=16?q=%.6f,%.6f", latitude, longitude, latitude, longitude));

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

        // Attempt to start an activity that can handle the Intent
        startActivity(mapIntent);
    }

    private void save() {
        if (check()) {
            EditText etAmount = requireView().findViewById(R.id.etAmount);
            String stringValue = etAmount.getText().toString();
            float amount = Float.parseFloat(stringValue);
            // Do something with the parsed number
            EditText etPurpose = requireView().findViewById(R.id.etPurpose);

            String categoryText;

            if (categoryGroup.getCheckedChipId() == View.NO_ID) {
                categoryText = "Uncategorized";
            } else {
                Chip category = requireView().findViewById(categoryGroup.getCheckedChipId());
                categoryText = category.getText().toString();
            }


            String base64String = null;

            if (imageBitmap != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream); // Compress bitmap to PNG format
                byte[] bitmapBytes = outputStream.toByteArray(); // Convert compressed bitmap to byte array
                base64String = Base64.encodeToString(bitmapBytes, Base64.DEFAULT); // Encode byte array to Base64 string
            }

            long currentTimeMillis = System.currentTimeMillis();

            Expense newExpense = new Expense(currentTimeMillis, amount, categoryText, etPurpose.getText().toString(), base64String, lon.get(), lat.get());

            new Thread(() -> {
                try {
                    ExpensesDao expensesDao = db.expensesDao();
                    expensesDao.insertAll(newExpense);

                    editor.putFloat("currentMonthlySpending", preferences.getFloat("currentMonthlySpending", 0.00f) + amount);
                    editor.putLong("lastRecord", currentTimeMillis);
                    editor.apply();

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reset();
                            update();
                        }
                    });
                } catch (NullPointerException npe) {
                    Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
                    Log.w("NPE", npe.toString());
                }
            }).start();
        }
    }

    // Define a method to show the image capture or picker dialog
    private void showImagePickerDialog() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }
}