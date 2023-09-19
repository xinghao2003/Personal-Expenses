package com.example.expenses;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

@Dao
interface ExpensesDao {
    @Query("SELECT * FROM expenses")
    List<Expense> getAll();


    @Query("SELECT * FROM expenses WHERE timeStamp LIKE (:timeStamps)")
    List<Expense> loadAllByTimeStamp(long timeStamps);

    /*
    @Query("SELECT * FROM expenses WHERE amount LIKE :first AND " +
            "purpose LIKE :last LIMIT 1")
    Expense findByName(String first, String last);
     */

    @Insert
    void insertAll(Expense... users);

    /*
    @Delete
    void delete(Expense user);

     */
}

@Entity(tableName = "expenses")
class Expense {
    @PrimaryKey
    @ColumnInfo(name = "timeStamp")
    public long timeStamp;

    @ColumnInfo(name = "amount")
    public float amount;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "purpose")
    public String purpose;

    @ColumnInfo(name = "evidence")
    public String evidence;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;


    public Expense(long timeStamp, float amount, String category, String purpose, String evidence, double longitude, double latitude) {
        this.timeStamp = timeStamp;
        this.amount = amount;
        this.category = category;
        this.purpose = purpose;
        this.evidence = evidence;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

@Database(entities = {Expense.class}, version = 3, exportSchema = false)
abstract class AppDatabase extends RoomDatabase {
    public abstract ExpensesDao expensesDao();
}


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeDatabase();

        // Find the view pager that will allow the user to swipe between fragments
        ViewPager2 viewPager = (ViewPager2) findViewById(R.id.viewpager);
        // Create an adapter that knows which fragment should be shown on each page
        FragmentStateAdapter adapter = new FragmentHandler(getSupportFragmentManager(), getLifecycle());
        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);
    }

    protected AppDatabase db = null;

    private void initializeDatabase(){
        final Migration MIGRATION_1_2 = new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE expenses "
                        + "ADD COLUMN category TEXT");

            }
        };

        final Migration MIGRATION_2_3 = new Migration(2, 3) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE expenses "
                        + "ADD COLUMN longitude REAL NOT NULL DEFAULT 0");
                database.execSQL("ALTER TABLE expenses "
                        + "ADD COLUMN latitude REAL NOT NULL DEFAULT 0");
            }
        };

        db = Room.databaseBuilder(this, AppDatabase.class, "personalExpenses").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
    }

    public AppDatabase accessDb(){
        return db;
    }
}
