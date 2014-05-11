package com.codepath.wwcmentorme.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.codepath.wwcmentorme.R;
import com.codepath.wwcmentorme.adapters.ChatAdapter;
import com.codepath.wwcmentorme.data.DataService;
import com.codepath.wwcmentorme.helpers.Async;
import com.codepath.wwcmentorme.models.Message;
import com.codepath.wwcmentorme.models.User;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingLeftInAnimationAdapter;
import com.parse.ParseException;
import com.parse.SaveCallback;

public class ChatActivity extends AppActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setAutohideActionBar(false);
		setContentView(R.layout.activity_chat);
		final long userId1 = getIntent().getLongExtra("userId1", 0);
		final long userId2 = getIntent().getLongExtra("userId2", 0);
		populateListView(userId1, userId2);
	}
	
	private void getMessages(final long userId1, final long userId2, int count, final ChatAdapter adapter, final ListView lv) {
		DataService.getMessages(userId1, userId2, 100, null, null, new Async.Block<List<Message>>() {
			@Override
			public void call(final List<Message> result) {
				if (result != null) {
					boolean scrollToBottom = false;
					if (adapter.getCount() == 0) {
						scrollToBottom = true;
					}
					addMessages(result, adapter);
					if (scrollToBottom) {
						lv.setSelection(adapter.getCount() - 1);
					}
				}
			}
		});
	}
	
	private void populateListView(final long userId1, final long userId2) {
		final ListView lv = (ListView) findViewById(R.id.lvChat);
		final ChatAdapter adapter = new ChatAdapter(getActivity());
		SwingLeftInAnimationAdapter scaleInAnimationAdapter = new SwingLeftInAnimationAdapter(adapter);
		scaleInAnimationAdapter.setAbsListView(lv);
		lv.setAdapter(scaleInAnimationAdapter);
		getMessages(userId1, userId2, 100, adapter, lv);
		final Runnable poll = new Runnable() {
			@Override
			public void run() {
				if (!getActivity().isFinishing() && !getActivity().destroyed()) {
					getMessages(userId1, userId2, 10, adapter, lv);
					Async.dispatchMain(this, 5000);
				}
			}
		};
		poll.run();
		final EditText etMessage = (EditText)findViewById(R.id.etMessage);
		final Button btSend = (Button)findViewById(R.id.btSend);
		final Runnable sendMessage = new Runnable() {
			@Override
			public void run() {
				final String text = etMessage.getText().toString().trim();
				if (text.length() > 0) {
					etMessage.setText(null);
					final String groupId = Message.getGroup(userId1, userId2);
					final Message message = new Message();
					message.setGroupId(groupId);
					message.setText(text);
					message.setUserId(User.meId());
					message.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								// Success.
								addMessage(message, adapter);
							} else {
								e.printStackTrace();
							}
						}
					});
				}
			}
		};
		etMessage.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(final TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					sendMessage.run();
					handled = true;
				}
				return handled;
			}
		});
		btSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage.run();
			}
		});
	}
	
	private void addMessage(final Message message, final ChatAdapter adapter) {
		final ArrayList<Message> list = new ArrayList<Message>();
		list.add(message);
		addMessages(list, adapter);
	}
	
	private void addMessages(final List<Message> messages, final ChatAdapter adapter) {
		Collections.reverse(messages);
		final List<ChatAdapter.MessageGroup> messageGroups = adapter.processMessages(messages);
		if (messageGroups.size() > 0) {
			adapter.addAll(messageGroups);
		}
		adapter.notifyDataSetChanged();
	}
}
