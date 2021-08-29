package comp5216.sydney.edu.au.todolist;

import org.apache.commons.io.FileUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    // Define variables
    ListView listView;
    SimpleAdapter itemsAdapter;
    EditText addItemEditText;

    ToDoItemDB db;
    ToDoItemDao toDoItemDao;

    List<Map<String, String>> items = new ArrayList<>();

    List<Map<String, String>> infos = new ArrayList<>();
    ActivityResultLauncher<Intent> mLauncher;

    String[] keys = {"title", "time"};
    int[] ids = {R.id.item_title, R.id.item_date};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use "activity_main.xml" as the layout
        setContentView(R.layout.activity_main);

        // Reference the "listView" variable to the id "lstView" in the layout
        listView = (ListView) findViewById(R.id.lstView);

        // Create an ArrayList of String
        //items = new ArrayList<String>();
        //items.add("item one");
        //items.add("item two");

        // Must call it before creating the adapter, because it references the right item list
        //readItemsFromFile();

        // Create an instance of ToDoItemDB and ToDoItemDao
        db = ToDoItemDB.getDatabase(this.getApplication().getApplicationContext());
        toDoItemDao = db.toDoItemDao();
        readItemsFromDatabase();
        // Sort view after load items.
        sortView();

        itemsAdapter = new SimpleAdapter(MainActivity.this, items, R.layout.listview_item, keys, ids);

        // Connect the listView and the adapter
        listView.setAdapter(itemsAdapter);

        // Setup listView listeners
        setupListViewListener();

        mLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Extract name value from result extras
                        String editedTitle = result.getData().getExtras().getString("title");
                        String editedTime = result.getData().getExtras().getString("time");
                        int position = result.getData().getIntExtra("position", -1);

                        Map<String, String> info = new HashMap<>();
                        info.put("title", editedTitle);
                        info.put("time", editedTime);

                        Map<String, String> item = new HashMap<>();
                        item.put("title", editedTitle);
                        item.put("time", getRemainTime(editedTime));

                        if (position == -1) {
                            infos.add(info);
                            items.add(item);
                        } else {
                            infos.set(position, info);
                            items.set(position, item);
                        }

                        // Sort view after update items.
                        sortView();

                        Log.i("Updated item in list ", editedTitle + ", time: " + editedTime + ", position: " + position);

                        // Make a standard toast that just contains text
                        Toast.makeText(getApplicationContext(), "Updated: " + editedTitle + ", time: " + editedTime, Toast.LENGTH_SHORT).show();

                        itemsAdapter.notifyDataSetChanged();

                        saveItemsToDatabase();
                    }
                }
        );
    }

    public void onAddItemClick(View view) {
        Log.i("MainActivity", "Add new Item!");

        Intent intent = new Intent(MainActivity.this, EditToDoItemActivity.class);

        // bring up the second activity
        mLauncher.launch(intent);
        itemsAdapter.notifyDataSetChanged();
    }

    private void setupListViewListener() {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long rowId) {
                Log.i("MainActivity", "Long Clicked item " + position);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_msg)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                items.remove(position); // Remove item from the ArrayList
                                infos.remove(position);
                                sortView();
                                itemsAdapter.notifyDataSetChanged(); // Notify listView adapter to update the list
                                saveItemsToDatabase();
                                Log.i("Delete an", " item " + position);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User cancelled the dialog
                                // Nothing happens
                            }
                        });

                builder.create().show();
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> item = (Map<String, String>) itemsAdapter.getItem(position);
                String title = (String) item.get("title");
                Log.i("MainActivity", "Clicked item " + position + ": " + title);

                Intent intent = new Intent(MainActivity.this, EditToDoItemActivity.class);
                if (intent != null) {
                    // put "extras" into the bundle for access in the edit activity
                    intent.putExtra("title", title);
                    String time = (String) infos.get(position).get("time");
                    intent.putExtra("time", time);
                    intent.putExtra("position", position);

                    // bring up the second activity
                    mLauncher.launch(intent);
                    itemsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void readItemsFromDatabase() {
        //Use asynchronous task to run query on the background and wait for result
        try {
            // Run a task specified by a Runnable Object asynchronously.
            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    //read items from database
                    List<ToDoItem> itemsFromDB = toDoItemDao.listAll();
                    System.out.println(itemsFromDB.size());

                    items = new ArrayList<Map<String, String>>();
                    infos = new ArrayList<Map<String, String>>();

                    if (itemsFromDB != null && itemsFromDB.size() > 0) {
                        for (ToDoItem item : itemsFromDB) {
                            Map<String, String> todo = new HashMap<>();
                            Map<String, String> info = new HashMap<>();
                            todo.put("title", (String) item.getToDoItemName());
                            info.put("title", (String) item.getToDoItemName());

                            String dateString = (String) item.getToDoItemTime();
                            if (dateString != null) {
                                todo.put("time", getRemainTime(dateString));
                                info.put("time", dateString);
                            }
                            items.add(todo);
                            infos.add(info);
                            Log.i("SQLite read item", "ID: " + item.getToDoItemID() + " Name: " + item.getToDoItemName() + "time");
                        }
                    }
                    System.out.println("I'll run in a separate thread than the main thread.");
                }
            });

            // Block and wait for the future to complete
            future.get();
        } catch (Exception ex) {
            Log.e("readItemsFromDatabase", ex.getStackTrace().toString());
        }
    }

    private void saveItemsToDatabase() {
        //Use asynchronous task to run query on the background to avoid locking UI
        // Sort view before persist items.
        sortView();
        try {
            // Run a task specified by a Runnable Object asynchronously.
            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    //delete all items and re-insert
                    toDoItemDao.deleteAll();
                    for (Map<String, String> todo : infos) {
                        ToDoItem item = new ToDoItem((String) todo.get("title"), (String) todo.get("time"));
                        toDoItemDao.insert(item);
                        Log.i("SQLite saved item", (String) todo.get("title"));
                    }
                    System.out.println("I'll run in a separate thread than the main thread.");
                }
            });
            // Block and wait for the future to complete
            future.get();
        } catch (Exception ex) {
            Log.e("saveItemsToDatabase", ex.getStackTrace().toString());
        }

    }

    private void printMap(Map<String, String> map) {
        StringBuilder str = new StringBuilder();
        for (String k : map.keySet()) {
            str.append(k).append(" - ").append(map.get(k)).append("     ");
        }
        System.out.println(str.toString());
    }

    /*
     *   Get remaining time to due
     *   "yyyy-MM-dd  hh:mm"  => "x days y hours z mins"
     * */
    private String getRemainTime(String dateString) {
        Date date = null;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd  hh:mm");
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String time = "";
        Date cur = new Date();
        if (cur.after(date)) {
            time = "OVERDUE";
        } else {
            long dateSecond = date.getTime() / 1000;
            long curSecond = cur.getTime() / 1000;
            long secondBetween = dateSecond - curSecond;
            long dayBetween = secondBetween / (3600 * 24);
            secondBetween -= dayBetween * 3600 * 24;
            long hourBetween = secondBetween / 3600;
            secondBetween -= hourBetween * 3600;
            long minBetween = secondBetween / 60;


            String str = dayBetween <= 1 ? " day" : " days";
            time += dayBetween + str;


            time += " ";
            str = hourBetween <= 1 ? " hour" : " hours";
            time += hourBetween + str;


            time += " ";
            str = minBetween <= 1 ? " min" : " mins";
            time += minBetween + str;

        }
        return time;
    }

    /*
     *   Inner class for sort.
     *
     * */
    class SortNode {
        int originIdx;
        Map<String, String> info;

        public SortNode(int idx, Map<String, String> info) {
            this.originIdx = idx;
            this.info = info;
        }

        public int compareTo(SortNode target) {
            String t1 = this.info.get("time");
            String t2 = target.info.get("time");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd  hh:mm");
            Date date1 = null;
            Date date2 = null;
            try {
                date1 = df.parse(t1);
                date2 = df.parse(t2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long dateMin1 = date1.getTime() / (1000 * 60);
            long dateMin2 = date2.getTime() / (1000 * 60);
            return (int) (dateMin1 - dateMin2);
        }
    }


    /*
     *   Sort the items and infos.
     * */
    private void sortView() {
        List<SortNode> list = new ArrayList<>();
        for (int i = 0; i < infos.size(); ++i) {
            list.add(new SortNode(i, infos.get(i)));
        }
        list.sort(new Comparator<SortNode>() {
            @Override
            public int compare(SortNode n1, SortNode n2) {
                return n1.compareTo(n2);
            }
        });
        List<Map<String, String>> sortedItems = new ArrayList<Map<String, String>>();
        List<Map<String, String>> sortedInfos = new ArrayList<Map<String, String>>();
        for (int i = 0; i < list.size(); ++i) {
            int idx = list.get(i).originIdx;
            sortedItems.add(items.get(idx));
            sortedInfos.add(infos.get(idx));
        }
        items = sortedItems;
        infos = sortedInfos;
        itemsAdapter = new SimpleAdapter(MainActivity.this, items, R.layout.listview_item, keys, ids);

        // Connect the listView and the adapter
        listView.setAdapter(itemsAdapter);
    }
}