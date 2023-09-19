package com.example.expenses;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HistoryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    protected AppDatabase db = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        RecyclerView rvHistory = rootView.findViewById(R.id.rvHistory);

        db = ((MainActivity) requireActivity()).accessDb();

        AtomicReference<List<Expense>> expenses = new AtomicReference<>();

        Thread t = new Thread(() -> {
            try {
                ExpensesDao expensesDao = db.expensesDao();
                expenses.set(expensesDao.getAll());

            } catch (NullPointerException npe) {
                Log.w("NPE", npe.toString());
                Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
            }
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Log.d("History Fragment", String.valueOf(expenses.get().size()));

        LinearLayoutManager llm = new LinearLayoutManager(requireContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rvHistory.setLayoutManager(llm);
        final RecyclerView.Adapter[] adapter = {new TransactionCard(expenses.get())};
        rvHistory.setAdapter(adapter[0]);
        SwipeRefreshLayout srHistory = rootView.findViewById(R.id.srHistory);
        srHistory.setOnRefreshListener(() -> {
            Thread t1 = new Thread(() -> {
                try {
                    ExpensesDao expensesDao = db.expensesDao();
                    expenses.set(expensesDao.getAll());

                } catch (NullPointerException npe) {
                    Log.w("NPE", npe.toString());
                    Toast.makeText(requireContext(), "Ops! Some error occur", Toast.LENGTH_SHORT).show();
                }
            });

            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            adapter[0] = new TransactionCard(expenses.get());
            rvHistory.setAdapter(adapter[0]);
            srHistory.setRefreshing(false);
        });
        // Inflate the layout for this fragment
        return rootView;
    }
}