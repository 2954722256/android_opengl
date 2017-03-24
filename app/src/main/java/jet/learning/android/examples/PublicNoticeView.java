package jet.learning.android.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.oglsamples.R;

public class PublicNoticeView extends LinearLayout{

	private static final String TAG = "LILITH"; 
	private Context mContext;
	private ViewFlipper viewFlipper;
	private View scrollTitleView;
	private Intent intent;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case 1:
				
				//bindNotices();
				break;

			case -1:
				break;
			}
		}
	};

	/**
	 * ����
	 * @param context
	 */
	public PublicNoticeView(Context context) {
		super(context);
		mContext = context;
		init();	
	}
	

	public PublicNoticeView(Context context,AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();
		
	}
	
	/**
	 * ��������󷵻ع������ݽ�������
	 */
	protected void bindNotices() {
		// TODO Auto-generated method stub
		viewFlipper.removeAllViews();
		int i = 0;
		while(i<5){
			String text = "����:�н��� 5000w-------";
			TextView textView = new TextView(mContext);
			textView.setText(text);
			textView.setOnClickListener(new NoticeTitleOnClickListener(mContext,i+text));
			LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			viewFlipper.addView(textView,lp);
			i++;
		}
	}


	private void init(){
		bindLinearLayout();
		Message msg = new Message();
		msg.what = 1;
		mHandler.sendMessageDelayed(msg, 3000);
		
	}

	/**
	 * ��ʼ���Զ���Ĳ���
	 */
	public void bindLinearLayout() {
//		scrollTitleView = LayoutInflater.from(mContext).inflate(R.layout.main_public_notice_title, null);
		LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		addView(scrollTitleView, layoutParams);

//		viewFlipper = (ViewFlipper) scrollTitleView.findViewById(R.id.flipper_scrollTitle);
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.slide_out_right));
		viewFlipper.startFlipping();
		View v = viewFlipper.getCurrentView();
		
	}
	
	
	/**
	 * ��ȡ������Ѷ
	 */
	public void getPublicNotices(){
		//���������ȡ
	}
	
	/**
	 * ����title����
	 * @author Nono
	 *
	 */
	class NoticeTitleOnClickListener implements OnClickListener{
		private Context context;
		private String titleid;

		public NoticeTitleOnClickListener(Context context, String whichText){
			this.context = context;
			this.titleid = whichText;
		}
		public void onClick(View v) {
			// TODO Auto-generated method stub
			disPlayNoticeContent(context,titleid);
		}
		
	}

	/**
	 * ��ʾnotice�ľ�������
	 * @param context
	 * @param titleid
	 */
	public void disPlayNoticeContent(Context context, String titleid) {
		// TODO Auto-generated method stub
		Toast.makeText(context, titleid, Toast.LENGTH_SHORT).show();
//		intent = new Intent(context, InformationContentActivity.class);
		intent.putExtra("tag", titleid);
		((Activity)context).startActivity(intent);
	}
}
