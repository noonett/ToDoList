package comp5216.sydney.edu.au.todolist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;


public class EditToDoItemActivity extends Activity implements View.OnTouchListener {
    public int position = 0;
    EditText etItem;
    EditText etTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Populate the screen using the layout
        setContentView(R.layout.activity_edit_item);

        // Get the data from the main activity screen
        String editItem = getIntent().getStringExtra("title");
        String editTime = getIntent().getStringExtra("time");
        position = getIntent().getIntExtra("position", -1);

        // Show original content in the text field
        etItem = (EditText) findViewById(R.id.etEditItem);
        etTime = (EditText) findViewById(R.id.etEditTime);
        etItem.setText(editItem);
        etTime.setText(editTime);
        etTime.setOnTouchListener(this);

    }

    public void onSubmit(View v) {

        etItem = (EditText) findViewById(R.id.etEditItem);

        // Prepare data intent for sending it back
        Intent data = new Intent();

        // Pass relevant data back as a result
        data.putExtra("title", etItem.getText().toString());
        data.putExtra("time", etTime.getText().toString());
        data.putExtra("position", position);

        // Activity finishes OK, return the data
        setResult(RESULT_OK, data); // Set result code and bundle data for response
        finish(); // Close the activity, pass data to parent
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            View view = View.inflate(this, R.layout.date_time_dialog, null);
            final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
            final TimePicker timePicker = (TimePicker) view.findViewById(R.id.time_picker);


            builder.setView(view);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());
            datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null);
            timePicker.setIs24HourView(true);
            timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
            timePicker.setCurrentMinute(Calendar.MINUTE);

            int inType = etTime.getInputType();
            etTime.setInputType(InputType.TYPE_NULL);
            etTime.onTouchEvent(event);
            etTime.setInputType(inType);
            etTime.setSelection(etTime.getText().length());


            builder.setTitle("Select Due Date");
            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                    StringBuffer sb = new StringBuffer();
                    sb.append(String.format("%d-%02d-%02d",
                            datePicker.getYear(),
                            datePicker.getMonth() + 1,
                            datePicker.getDayOfMonth()));
                    sb.append("  ");
                    sb.append(timePicker.getCurrentHour())
                            .append(":").append(timePicker.getCurrentMinute());
                    etTime.setText(sb);

                    dialog.cancel();
                }
            });


            Dialog dialog = builder.create();
            dialog.show();
        }

        return true;
    }
}
