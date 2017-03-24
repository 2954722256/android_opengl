package jet.learning.android.examples;

import java.text.SimpleDateFormat;

import jet.learning.android.examples.DateTimePickerDialog.OnDateTimeSetListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.oglsamples.R;
 
public class DateTimePickerActivity extends Activity {
    private Button btn;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) this.findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
    }
 
    public void showDialog() {
        DateTimePickerDialog dialog = new DateTimePickerDialog(this,
                System.currentTimeMillis());
        /**
         * ʵ�ֽӿ�
         */
        dialog.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                Toast.makeText(DateTimePickerActivity.this,
                        "������������ǣ�" + getStringDate(date), Toast.LENGTH_LONG)
                        .show();
            }
        });
        dialog.show();
    }
 
    /**
     * ����ʱ���ʽ�ַ���ת��Ϊʱ�� yyyy-MM-dd HH:mm:ss
     * 
     */
    public static String getStringDate(Long date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(date);
        return dateString;
    }
 
}
