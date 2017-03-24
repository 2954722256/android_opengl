package jet.learning.android.examples;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.oglsamples.R;

public class ScrollListViewActivity extends Activity{

	Button   button;
	ScrollView listview;
	int cursor = 0;
	int viewHeight = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_listview);
		
		button = (Button) findViewById(R.id.button);
		listview = (ScrollView) findViewById(R.id.listview);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		adapter.add("��ɮ");
		adapter.add("�����");
		adapter.add("��˽�");
		adapter.add("ɳɮ");
		adapter.add("������");
		adapter.add("��̫��");
		adapter.add("�Ƹ���");
		adapter.add("����������");
		adapter.add("��ʴ��");
		adapter.add("��������");
		adapter.add("̫���Ͼ�");
		adapter.add("����������");
		adapter.add("��߸");
		adapter.add("̫�׽���");
		adapter.add("����Ԫ˧");
		adapter.add("�캢��");
		adapter.add("��������");
		adapter.add("����");
		adapter.add("ξ�پ���");
		adapter.add("����Ԫ˧");
		
		final LinearLayout container = (LinearLayout) findViewById(R.id.container);
		final int count = adapter.getCount();
		for(int i = 0; i < count; i++){
			container.addView(adapter.getView(i, null, container));
		}
		
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int height = container.getMeasuredHeight()/count;
				
				cursor++;
//				listview.smoothScrollToPosition(cursor);
				if(cursor < count){
//					listview.scrollBy(0, 50);
//					listview.smoothScrollToPosition(cursor);
					listview.smoothScrollTo(0, height * cursor);
				}else{
					cursor = 0;
					listview.scrollTo(0, 0);
				}
			}
		});
		
		
	}
}
